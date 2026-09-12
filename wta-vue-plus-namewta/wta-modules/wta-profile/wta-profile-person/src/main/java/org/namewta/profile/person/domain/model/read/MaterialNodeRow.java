package org.namewta.profile.person.domain.model.read;

/** 材料目录节点查询读模型。 */
public record MaterialNodeRow(Long materialNodeId, Long parentId, String nodeType, Integer nodeDepth,
                              String profileType, String materialTagCode, String nodeName,
                              String systemRequired, String status, Integer orderNum, Integer version) {
}
