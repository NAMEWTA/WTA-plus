package org.namewta.profile.person.service;
import org.namewta.profile.person.domain.exception.ProfileMaterialException;
import org.namewta.profile.person.domain.material.MaterialNode;
import org.namewta.profile.person.domain.material.MaterialOwner;
import org.namewta.profile.person.domain.material.MaterialReference;
import org.namewta.profile.person.domain.material.MaterialRequirement;
import org.namewta.profile.person.domain.model.read.MaterialNodeRow;
import org.namewta.profile.person.domain.model.read.MaterialReferenceRow;
import org.namewta.profile.person.dao.ProfileMaterialDao;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.ResolvedMaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialOwnerContributor.SnapshotRelationship;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialAttachCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeView;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialScope;
import org.namewta.system.api.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * 创建档案材料业务服务。
 */
@Service
public class ProfileMaterialService implements IProfileMaterialService {
    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    static final int MAX_FILE_COUNT = 10;
    private static final String REFERENCE_TABLE = "profile_material_ref";
    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
        ".jpg", Set.of("image/jpeg"),
        ".jpeg", Set.of("image/jpeg"),
        ".png", Set.of("image/png"),
        ".pdf", Set.of("application/pdf"));
    private final ProfileMaterialDao dao;
    private final OssService ossService;
    private final ProfileMaterialAccessPolicy accessPolicy;
    private final Map<ProfileType, ProfileMaterialOwnerContributor> ownerContributors;
    private final Clock clock;
    /** 创建档案材料业务服务。 */
    @Autowired
    public ProfileMaterialService(ProfileMaterialDao dao, OssService ossService,
                                      ProfileMaterialAccessPolicy accessPolicy,
                                      List<ProfileMaterialOwnerContributor> ownerContributors) {
        this(dao, ossService, accessPolicy, ownerContributors, Clock.systemUTC());
    }
    /** 创建可注入时钟的档案材料业务服务，测试场景据此固定时间。 */
    public ProfileMaterialService(ProfileMaterialDao dao, OssService ossService,
                               ProfileMaterialAccessPolicy accessPolicy,
                               List<ProfileMaterialOwnerContributor> ownerContributors,
                               Clock clock) {
        this.dao = dao;
        this.ossService = ossService;
        this.accessPolicy = accessPolicy;
        this.ownerContributors = ownerContributors.stream().collect(Collectors.toUnmodifiableMap(
            ProfileMaterialOwnerContributor::profileType, Function.identity()));
        this.clock = clock;
    }
    /**
     * 查询材料目录树
     */
    public List<MaterialNodeView> tree(MaterialScope scope, boolean includeDisabled) {
        Objects.requireNonNull(scope, "scope");
        if (includeDisabled) {
            accessPolicy.requireCatalogRead();
        }
        return buildTree(nodes(scope, includeDisabled));
    }
    /**
     * 创建材料节点
     */

    public MaterialNodeView createNode(MaterialNodeCommand command) {
        accessPolicy.requireCatalogManage();
        Shape shape = shape(command, null);
        return view(insertNode(IdGeneratorUtil.nextLongId(), command, shape.depth()), List.of());
    }
    /**
     * 更新node。
     */

    public MaterialNodeView updateNode(Long materialNodeId, MaterialNodeCommand command) {
        accessPolicy.requireCatalogManage();
        MaterialNode current = requireNodeForUpdate(materialNodeId);
        protectSystemCode(current, command);
        Shape shape = shape(command, current);
        return view(updateNode(materialNodeId, command, shape.depth()), List.of());
    }
    /**
     * 变更材料节点状态
     */

    public void changeStatus(Long materialNodeId, boolean enabled, int expectedVersion) {
        accessPolicy.requireCatalogManage();
        MaterialNode current = requireNodeForUpdate(materialNodeId);
        if (!enabled && (current.systemRequired() || current.nodeDepth() == 1)) {
            throw failure("SYSTEM_MATERIAL_TAG_PROTECTED");
        }
        if (!enabled && current.nodeType() == MaterialNodeType.CATEGORY && countChildren(materialNodeId) > 0) {
            throw failure("MATERIAL_CATEGORY_NOT_EMPTY");
        }
        changeNodeStatus(materialNodeId, enabled, expectedVersion);
    }
    /**
     * 归档材料节点
     */

    public void archiveNode(Long materialNodeId, int expectedVersion) {
        accessPolicy.requireCatalogManage();
        MaterialNode current = requireNodeForUpdate(materialNodeId);
        if (current.systemRequired() || current.nodeDepth() == 1) {
            throw failure("SYSTEM_MATERIAL_TAG_PROTECTED");
        }
        if (countChildren(materialNodeId) > 0 || countReferences(materialNodeId) > 0) {
            throw failure("MATERIAL_NODE_IN_USE");
        }
        archiveMaterialNode(materialNodeId, expectedVersion);
    }
    /**
     * 关联材料到业务对象
     */

    public MaterialReferenceView attach(MaterialAttachCommand command) {
        MaterialOwner owner = lockOwner(command.owner());
        MaterialOwnerType ownerType = command.owner().ownerType();
        if (ownerType != MaterialOwnerType.WORKING && ownerType != MaterialOwnerType.SOURCE) {
            throw failure("IMMUTABLE_MATERIAL_OWNER");
        }
        Long authorizedUploaderUserId = accessPolicy.requireAttach(owner);
        if (ownerType == MaterialOwnerType.WORKING) {
            requireWorkingEditable(command.owner());
        }
        MaterialNode tag = requireNode(command.materialNodeId());
        requireApplicableTag(command.owner().profileType(), tag);
        if (countAttached(command.owner()) >= MAX_FILE_COUNT) {
            throw failure("MATERIAL_COUNT_LIMIT");
        }
        OssService.OssObjectMetadata metadata = ossService.objectMetadata(command.ossId());
        if (!Objects.equals(authorizedUploaderUserId, metadata.uploaderUserId())) {
            throw failure("MATERIAL_OSS_OWNER_MISMATCH");
        }
        String extension = validateMetadata(metadata);
        Instant now = clock.instant();
        /**
         * 规范化文件 MIME 类型
         */
        MaterialReference reference = new MaterialReference(IdGeneratorUtil.nextLongId(), command.owner(), command.ossId(),
            tag.materialNodeId(), tag.materialTagCode(), tag.nodeName(), metadata.fileName(), metadata.fileSize(),
            extension, normalizeMime(metadata.contentType()), true,
            ownerType == MaterialOwnerType.SOURCE, now, null, 0);
        MaterialReference inserted = insertReference(reference);
        ossService.reconcileReferences(REFERENCE_TABLE, String.valueOf(inserted.materialRefId()),
            Set.of(), Set.of(inserted.ossId()));
        return view(inserted);
    }
    /**
     * 解除业务对象与材料的关联
     */

    public void detach(MaterialOwnerKey ownerKey, Long materialRefId) {
        MaterialOwner owner = lockOwner(ownerKey);
        accessPolicy.requireWrite(owner);
        if (ownerKey.ownerType() != MaterialOwnerType.WORKING) {
            throw failure("IMMUTABLE_MATERIAL");
        }
        requireWorkingEditable(ownerKey);
        MaterialReference reference = requireReference(materialRefId);
        requireSameOwner(ownerKey, reference);
        if (reference.immutableEvidence()) {
            throw failure("IMMUTABLE_MATERIAL");
        }
        if (!reference.attached()) {
            return;
        }
        detachReference(materialRefId, clock.instant());
        ossService.reconcileReferences(REFERENCE_TABLE, String.valueOf(materialRefId),
            Set.of(reference.ossId()), Set.of());
    }
    /**
     * 查询材料列表
     */

    public List<MaterialReferenceView> list(MaterialOwnerKey ownerKey) {
        MaterialOwner owner = lockOwner(ownerKey);
        accessPolicy.requireRead(owner);
        return references(ownerKey).stream().map(this::view).toList();
    }
    /**
     * 生成材料访问地址
     */

    public OssService.OssAccessUrl accessUrl(MaterialOwnerKey ownerKey, Long materialRefId) {
        MaterialOwner owner = lockOwner(ownerKey);
        accessPolicy.requireRead(owner);
        MaterialReference reference = requireReference(materialRefId);
        requireSameOwner(ownerKey, reference);
        if (!reference.attached() && ownerKey.ownerType() == MaterialOwnerType.WORKING) {
            throw failure("MATERIAL_NOT_ATTACHED");
        }
        return ossService.resolveAccessUrl(reference.ossId());
    }
    /** 查询材料访问地址并转换为 Profile HTTP 输出。 */
    public org.namewta.profile.person.domain.vo.PersonProfileAccessUrl accessUrlView(MaterialOwnerKey ownerKey,
                                                                                  Long materialRefId) {
        OssService.OssAccessUrl value = accessUrl(ownerKey, materialRefId);
        return value == null ? null : new org.namewta.profile.person.domain.vo.PersonProfileAccessUrl(
            value.accessType(), value.url(), value.expiresAt(), value.fileName());
    }
    /**
     * 校验必需材料是否齐全
     */

    public void validateRequired(MaterialOwnerKey ownerKey, String documentTypeCode, Set<String> conditions) {
        MaterialOwner owner = lockOwner(ownerKey);
        accessPolicy.requireRead(owner);
        Set<String> normalizedConditions = conditions == null || conditions.isEmpty()
            ? Set.of("ALWAYS") : Set.copyOf(conditions);
        Map<String, Long> counts = attachedCountsByTag(ownerKey);
        for (MaterialRequirement requirement : requirements(
            ownerKey.profileType(), requireText(documentTypeCode, "documentTypeCode"), normalizedConditions)) {
            if (counts.getOrDefault(requirement.materialTagCode(), 0L) < requirement.minimumCount()) {
                throw failure("MISSING_REQUIRED_MATERIAL:" + requirement.materialTagCode());
            }
        }
    }
    /**
     * 冻结材料快照数据
     */

    public List<MaterialReferenceView> snapshotImmutable(MaterialOwnerKey source, MaterialOwnerKey target) {
        if (source.profileType() != target.profileType() || !validSnapshotTransition(source, target)) {
            throw failure("MATERIAL_SNAPSHOT_OWNER_INVALID");
        }
        MaterialOwner sourceOwner = lockOwner(source);
        if (source.ownerType() == MaterialOwnerType.WORKING) {
            requireWorkingEditable(source);
            accessPolicy.requireWrite(sourceOwner);
        }
        MaterialOwner targetOwner = lockOwner(target);
        if (targetOwner.applicantUserId() != null
            && !Objects.equals(sourceOwner.applicantUserId(), targetOwner.applicantUserId())) {
            throw failure("MATERIAL_SNAPSHOT_OWNER_INVALID");
        }
        if (!ownerContributor(source.profileType()).hasSnapshotRelationship(
            new SnapshotRelationship(source, target))) {
            throw failure("MATERIAL_SNAPSHOT_OWNER_INVALID");
        }
        return insertImmutableCopies(source, target, clock.instant()).stream().map(reference -> {
            ossService.reconcileReferences(REFERENCE_TABLE, String.valueOf(reference.materialRefId()),
                Set.of(), Set.of(reference.ossId()));
            return view(reference);
        }).toList();
    }
    /**
     * 校验快照状态转换是否合法
     */
    private boolean validSnapshotTransition(MaterialOwnerKey source, MaterialOwnerKey target) {
        return (source.ownerType() == MaterialOwnerType.WORKING
            && target.ownerType() == MaterialOwnerType.SUBMISSION)
            || (source.ownerType() == MaterialOwnerType.SUBMISSION
            && target.ownerType() == MaterialOwnerType.VERSION)
            || (source.ownerType() == MaterialOwnerType.SOURCE
            && target.ownerType() == MaterialOwnerType.VERSION);
    }
    /**
     * 锁定材料所有者记录
     */
    private MaterialOwner lockOwner(MaterialOwnerKey owner) {
        ResolvedMaterialOwner resolved = ownerContributor(owner.profileType()).lockOwner(owner)
            .orElseThrow(() -> failure("MATERIAL_OWNER_NOT_FOUND"));
        return new MaterialOwner(resolved.owner(), resolved.applicantUserId());
    }
    /**
     * 校验档案处于可编辑状态
     */
    private void requireWorkingEditable(MaterialOwnerKey owner) {
        if (!ownerContributor(owner.profileType()).isWorkingEditable(owner)) {
            throw failure("MATERIAL_OWNER_READ_ONLY");
        }
    }
    /**
     * 解析对应类型的材料所有者处理器
     */
    private ProfileMaterialOwnerContributor ownerContributor(ProfileType profileType) {
        ProfileMaterialOwnerContributor contributor = ownerContributors.get(profileType);
        if (contributor == null) {
            throw failure("MATERIAL_OWNER_NOT_FOUND");
        }
        return contributor;
    }
    /**
     * 校验并计算材料节点结构
     */
    private Shape shape(MaterialNodeCommand command, MaterialNode current) {
        String nodeName = requireText(command.nodeName(), "nodeName");
        if (current == null && command.systemRequired()) {
            throw failure("SYSTEM_MATERIAL_TAG_PROTECTED");
        }
        if (nodeName.length() > 100 || command.parentId() < 0 || command.orderNum() < 0) {
            throw failure("MATERIAL_NODE_INVALID");
        }
        if (command.parentId() == 0) {
            if (current == null) {
                throw failure("MATERIAL_ROOT_SCOPE_FIXED");
            }
            if (command.nodeType() != MaterialNodeType.CATEGORY || command.materialTagCode() != null
                || command.systemRequired()) {
                throw failure("MATERIAL_TREE_SHAPE_INVALID");
            }
            return new Shape(1);
        }
        MaterialNode parent = requireNode(command.parentId());
        if (parent.nodeType() != MaterialNodeType.CATEGORY || !parent.enabled()) {
            throw failure("MATERIAL_TREE_PARENT_INVALID");
        }
        if (parent.scope() != command.scope()) {
            throw failure("MATERIAL_TREE_PROFILE_MISMATCH");
        }
        int depth = parent.nodeDepth() + 1;
        if (command.nodeType() == MaterialNodeType.CATEGORY) {
            if (depth != 2 || command.materialTagCode() != null || command.systemRequired()) {
                throw failure("MATERIAL_TREE_SHAPE_INVALID");
            }
        } else if ((depth != 2 && depth != 3) || command.materialTagCode() == null
            || !command.materialTagCode().matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw failure("MATERIAL_TREE_SHAPE_INVALID");
        }
        if (current != null && current.nodeType() != command.nodeType()) {
            throw failure("MATERIAL_NODE_TYPE_IMMUTABLE");
        }
        return new Shape(depth);
    }
    /**
     * 保护系统内置材料编码
     */
    private void protectSystemCode(MaterialNode current, MaterialNodeCommand command) {
        if (current.scope() != command.scope()) {
            throw failure("MATERIAL_NODE_SCOPE_IMMUTABLE");
        }
        if (current.nodeType() == MaterialNodeType.TAG
            && !Objects.equals(current.materialTagCode(), command.materialTagCode())) {
            throw failure(current.systemRequired()
                ? "SYSTEM_MATERIAL_TAG_PROTECTED" : "MATERIAL_TAG_CODE_IMMUTABLE");
        }
        if (current.systemRequired() && !command.systemRequired()) {
            throw failure("SYSTEM_MATERIAL_TAG_PROTECTED");
        }
    }
    /**
     * 校验材料标签适用范围
     */
    private void requireApplicableTag(ProfileType ownerType, MaterialNode tag) {
        if (tag.nodeType() != MaterialNodeType.TAG || !tag.enabled()) {
            throw failure("MATERIAL_TAG_UNAVAILABLE");
        }
        if (tag.scope() != MaterialScope.COMMON && !tag.scope().name().equals(ownerType.name())) {
            throw failure("MATERIAL_TAG_NOT_APPLICABLE");
        }
    }
    /**
     * 校验材料元数据
     */
    private String validateMetadata(OssService.OssObjectMetadata metadata) {
        if (metadata.fileSize() <= 0 || metadata.fileSize() > MAX_FILE_SIZE) {
            throw failure("MATERIAL_FILE_TOO_LARGE");
        }
        String originalExtension = extension(metadata.fileName());
        String storedExtension = normalizeExtension(metadata.fileSuffix());
        String mime = normalizeMime(metadata.contentType());
        if (!originalExtension.equals(storedExtension)
            || !ALLOWED_TYPES.getOrDefault(originalExtension, Set.of()).contains(mime)) {
            throw failure("MATERIAL_FILE_TYPE_INVALID");
        }
        return originalExtension;
    }
    /**
     * 处理extension。
     */
    private String extension(String fileName) {
        String value = requireText(fileName, "fileName");
        int index = value.lastIndexOf('.');
        return index < 0 ? "" : normalizeExtension(value.substring(index));
    }
    /**
     * 规范化文件扩展名
     */
    private String normalizeExtension(String extension) {
        String value = requireText(extension, "fileSuffix").toLowerCase(Locale.ROOT);
        return value.startsWith(".") ? value : "." + value;
    }
    /**
     * 规范化文件 MIME 类型
     */
    private String normalizeMime(String mime) {
        return requireText(mime, "contentType").toLowerCase(Locale.ROOT);
    }
    /**
     * 查询材料节点
     */
    private List<MaterialNode> nodes(MaterialScope scope, boolean includeDisabled) {
        return dao.selectNodes(scope.name(), includeDisabled).stream().map(this::node).toList();
    }
    /**
     * 校验并获取材料节点
     */
    private MaterialNode requireNode(Long materialNodeId) {
        return requireNode(dao.selectNode(requirePositive(materialNodeId, "materialNodeId")));
    }
    /**
     * 校验并获取待更新材料节点
     */
    private MaterialNode requireNodeForUpdate(Long materialNodeId) {
        return requireNode(dao.lockNode(requirePositive(materialNodeId, "materialNodeId")));
    }
    /**
     * 新增材料节点记录
     */
    private MaterialNode insertNode(Long materialNodeId, MaterialNodeCommand command, int depth) {
        try {
            dao.insertNode(materialNodeId, command.parentId(), command.nodeType().name(), depth,
                command.scope().name(), command.materialTagCode(), command.nodeName().strip(),
                command.systemRequired() ? "Y" : "N", command.orderNum());
        } catch (DuplicateKeyException exception) {
            throw failure("MATERIAL_TAG_CODE_CONFLICT", exception);
        }
        return requireNode(materialNodeId);
    }
    /**
     * 更新node。
     */
    private MaterialNode updateNode(Long materialNodeId, MaterialNodeCommand command, int depth) {
        try {
            requireChanged(dao.updateNode(materialNodeId, command.parentId(), depth, command.scope().name(),
                command.materialTagCode(), command.nodeName().strip(), command.systemRequired() ? "Y" : "N",
                command.orderNum(), command.expectedVersion()));
        } catch (DuplicateKeyException exception) {
            throw failure("MATERIAL_TAG_CODE_CONFLICT", exception);
        }
        return requireNode(materialNodeId);
    }
    /**
     * 统计材料节点的子节点数量
     */
    private long countChildren(Long materialNodeId) {
        return dao.countChildren(materialNodeId);
    }
    /**
     * 统计材料引用数量
     */
    private long countReferences(Long materialNodeId) {
        return dao.countReferences(materialNodeId);
    }
    /**
     * 变更材料节点状态
     */
    private void changeNodeStatus(Long materialNodeId, boolean enabled, int expectedVersion) {
        requireChanged(dao.updateStatus(materialNodeId, enabled ? "0" : "1", expectedVersion));
    }
    /**
     * 归档材料节点及其关联数据
     */
    private void archiveMaterialNode(Long materialNodeId, int expectedVersion) {
        requireChanged(dao.archiveNode(materialNodeId, expectedVersion));
    }
    /**
     * 统计已关联材料数量
     */
    private long countAttached(MaterialOwnerKey owner) {
        return dao.countAttached(owner.profileType().name(), owner.ownerType().name(), owner.ownerId());
    }
    /**
     * 新增材料引用记录
     */
    private MaterialReference insertReference(MaterialReference reference) {
        try {
            requireChanged(dao.insertReference(row(reference)));
            return reference;
        } catch (DuplicateKeyException exception) {
            throw failure("MATERIAL_ALREADY_ATTACHED", exception);
        }
    }
    /**
     * 校验并获取材料引用
     */
    private MaterialReference requireReference(Long materialRefId) {
        MaterialReferenceRow row = dao.lockReference(requirePositive(materialRefId, "materialRefId"));
        if (row == null) {
            throw failure("MATERIAL_NOT_FOUND");
        }
        return reference(row);
    }
    /**
     * 解除材料引用关系
     */
    private void detachReference(Long materialRefId, Instant detachedTime) {
        requireChanged(dao.detachReference(materialRefId, detachedTime));
    }
    /**
     * 查询材料引用记录
     */
    private List<MaterialReference> references(MaterialOwnerKey owner) {
        return dao.selectReferences(owner.profileType().name(), owner.ownerType().name(), owner.ownerId())
            .stream().map(this::reference).toList();
    }
    /**
     * 处理requirements。
     */
    private List<MaterialRequirement> requirements(ProfileType profileType, String documentTypeCode,
                                                    Set<String> conditions) {
        return dao.selectRequirements(profileType.name(), documentTypeCode, conditions).stream()
            .map(row -> new MaterialRequirement(row.materialTagCode(), row.minimumCount())).toList();
    }
    /**
     * 统计标签下的材料数量
     */
    private Map<String, Long> attachedCountsByTag(MaterialOwnerKey owner) {
        Map<String, Long> counts = new LinkedHashMap<>();
        dao.selectAttachedCounts(owner.profileType().name(), owner.ownerType().name(), owner.ownerId())
            .forEach(row -> counts.put(row.materialTagCode(), row.materialCount()));
        return Map.copyOf(counts);
    }
    /**
     * 新增不可变材料副本
     */
    private List<MaterialReference> insertImmutableCopies(MaterialOwnerKey source, MaterialOwnerKey target,
                                                           Instant attachedTime) {
        return references(source).stream().filter(MaterialReference::attached).map(existing -> {
            MaterialReference copy = new MaterialReference(IdGeneratorUtil.nextLongId(), target, existing.ossId(),
                existing.materialNodeId(), existing.materialTagCode(), existing.materialTagName(),
                existing.fileName(), existing.fileSize(), existing.fileExtension(), existing.mimeType(),
                true, true, attachedTime, null, 0);
            return insertReference(copy);
        }).toList();
    }
    /**
     * 组装材料节点数据
     */
    private MaterialNode node(MaterialNodeRow row) {
        return new MaterialNode(row.materialNodeId(), row.parentId(), MaterialNodeType.valueOf(row.nodeType()),
            row.nodeDepth(), MaterialScope.valueOf(row.profileType()), row.materialTagCode(), row.nodeName(),
            "Y".equals(row.systemRequired()), "0".equals(row.status()), row.orderNum(), row.version());
    }
    /**
     * 校验并获取材料节点
     */
    private MaterialNode requireNode(MaterialNodeRow row) {
        if (row == null) {
            throw failure("MATERIAL_NODE_NOT_FOUND");
        }
        return node(row);
    }
    /**
     * 组装材料引用数据
     */
    private MaterialReference reference(MaterialReferenceRow row) {
        MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.valueOf(row.profileType()),
            MaterialOwnerType.valueOf(row.ownerType()), row.ownerId());
        return new MaterialReference(row.materialRefId(), owner, row.ossId(), row.materialNodeId(),
            row.materialTagCode(), row.materialTagName(), row.fileName(), row.fileSize(), row.fileExtension(),
            row.mimeType(), "ATTACHED".equals(row.status()), "Y".equals(row.immutableFlag()),
            row.attachedTime(), row.detachedTime(), row.version());
    }
    /**
     * 转换持久化读模型
     */
    private MaterialReferenceRow row(MaterialReference reference) {
        return new MaterialReferenceRow(reference.materialRefId(), reference.owner().ownerType().name(),
            reference.owner().ownerId(), reference.owner().profileType().name(), reference.ossId(),
            reference.materialNodeId(), reference.materialTagCode(), reference.materialTagName(),
            reference.fileName(), reference.fileSize(), reference.fileExtension(), reference.mimeType(),
            reference.attached() ? "ATTACHED" : "DETACHED", reference.immutableEvidence() ? "Y" : "N",
            reference.attachedTime(), reference.detachedTime(), reference.version());
    }
    /**
     * 校验编号为正数
     */
    private long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw failure(field + "_INVALID");
        }
        return value;
    }
    /**
     * 校验申请确实发生变更
     */
    private void requireChanged(int changed) {
        if (changed != 1) {
            throw failure("MATERIAL_VERSION_CONFLICT");
        }
    }
    /**
     * 构建材料目录树
     */
    private List<MaterialNodeView> buildTree(List<MaterialNode> nodes) {
        Map<Long, List<MaterialNode>> children = new HashMap<>();
        nodes.forEach(node -> children.computeIfAbsent(node.parentId(), ignored -> new ArrayList<>()).add(node));
        Comparator<MaterialNode> order = Comparator.comparingInt(MaterialNode::orderNum)
            .thenComparing(MaterialNode::materialNodeId);
        children.values().forEach(list -> list.sort(order));
        return children.getOrDefault(0L, List.of()).stream().map(node -> treeView(node, children)).toList();
    }
    /**
     * 转换材料目录树视图
     */
    private MaterialNodeView treeView(MaterialNode node, Map<Long, List<MaterialNode>> children) {
        return view(node, children.getOrDefault(node.materialNodeId(), List.of()).stream()
            .map(child -> treeView(child, children)).toList());
    }
    /**
     * 转换为对外视图对象
     */
    private MaterialNodeView view(MaterialNode node, List<MaterialNodeView> children) {
        return new MaterialNodeView(node.materialNodeId(), node.parentId(), node.nodeType(), node.nodeDepth(),
            node.scope(), node.materialTagCode(), node.nodeName(), node.systemRequired(), node.enabled(),
            node.orderNum(), node.version(), children);
    }
    /**
     * 转换为对外视图对象
     */
    private MaterialReferenceView view(MaterialReference reference) {
        return new MaterialReferenceView(reference.materialRefId(), reference.owner(), reference.ossId(),
            reference.materialNodeId(), reference.materialTagCode(), reference.materialTagName(), reference.fileName(),
            reference.fileSize(), reference.fileExtension(), reference.mimeType(), reference.attached(),
            reference.immutableEvidence(), reference.attachedTime(), reference.detachedTime(), reference.version());
    }
    /**
     * 校验材料属于同一所有者
     */
    private void requireSameOwner(MaterialOwnerKey owner, MaterialReference reference) {
        if (!owner.equals(reference.owner())) {
            throw failure("MATERIAL_NOT_FOUND");
        }
    }
    /**
     * 校验文本非空
     */
    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw failure("MATERIAL_" + field.toUpperCase(Locale.ROOT) + "_INVALID");
        }
        return value.strip();
    }
    /**
     * 构造业务失败异常
     */
    private ProfileMaterialException failure(String category) {
        return new ProfileMaterialException(category);
    }
    /**
     * 构造业务失败异常
     */
    private ProfileMaterialException failure(String category, Throwable cause) {
        ProfileMaterialException exception = new ProfileMaterialException(category);
        exception.initCause(cause);
        return exception;
    }
    /**
     * 承载Shape业务规则的领域服务。
     */
    private record Shape(int depth) {
    }
}
