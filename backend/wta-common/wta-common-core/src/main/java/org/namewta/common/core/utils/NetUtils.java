package org.namewta.common.core.utils;

import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.NetUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.utils.regex.RegexUtils;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 增强网络相关工具类
 *
 * @author 秋辞未寒
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NetUtils extends NetUtil {

    /**
     * 判断是否为IPv6地址
     *
     * @param ip IP地址
     * @return 是否为IPv6地址
     */
    public static boolean isIPv6(String ip) {
        try {
            // 判断是否为IPv6地址
            return InetAddress.getByName(ip) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 判断IPv6地址是否为内网地址
     * <br><br>
     * 以下地址将归类为本地地址，如有业务场景有需要，请根据需求自行处理：
     * <pre>
     * 通配符地址 0:0:0:0:0:0:0:0
     * 链路本地地址 fe80::/10
     * 唯一本地地址 fec0::/10
     * 环回地址 ::1
     * </pre>
     *
     * @param ip IP地址
     * @return 是否为内网地址
     */
    public static boolean isInnerIPv6(String ip) {
        try {
            // 判断是否为IPv6地址
            InetAddress inetAddress = InetAddress.getByName(ip);
            if (inetAddress instanceof Inet6Address inet6Address) {
                // isAnyLocalAddress 判断是否为通配符地址，通常不会将其视为内网地址，根据业务场景自行处理判断
                // isLinkLocalAddress 判断是否为链路本地地址，通常不算内网地址，是否划分归属于内网需要根据业务场景自行处理判断
                // isLoopbackAddress 判断是否为环回地址，与IPv4的 127.0.0.1 同理，用于表示本机
                // isSiteLocalAddress 判断是否为本地站点地址，IPv6唯一本地地址（Unique Local Addresses，简称ULA）
                if (inet6Address.isAnyLocalAddress()
                    || inet6Address.isLinkLocalAddress()
                    || inet6Address.isLoopbackAddress()
                    || inet6Address.isSiteLocalAddress()) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // 注意，isInnerIPv6方法和isIPv6方法的适用范围不同，所以此处不能忽略其异常信息。
            throw new IllegalArgumentException("Invalid IPv6 address!", e);
        }
        return false;
    }

    /**
     * 判断是否为IPv4地址
     *
     * @param ip IP地址
     * @return 是否为IPv4地址
     */
    public static boolean isIPv4(String ip) {
        return RegexUtils.isMatch(PatternPool.IPV4, ip);
    }

    /**
     * 匹配 IP 规则，支持精确值、通配符与 CIDR。
     *
     * @param rule     IP 规则
     * @param clientIp 客户端 IP
     * @return 是否匹配
     */
    public static boolean isMatchIpRule(String rule, String clientIp) {
        if (StringUtils.isBlank(rule) || StringUtils.isBlank(clientIp)) {
            return false;
        }
        String ipRule = StringUtils.trim(rule);
        if (StringUtils.equals(ipRule, clientIp)) {
            return true;
        }
        if (ipRule.contains("/")) {
            return isMatchCidr(ipRule, clientIp);
        }
        if (StringUtils.containsAny(ipRule, "*", "?")) {
            String regex = ipRule
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
            return clientIp.matches(regex);
        }
        try {
            return Arrays.equals(parseIpLiteral(ipRule).getAddress(), parseIpLiteral(clientIp).getAddress());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * 匹配 CIDR 网段。
     *
     * @param cidr     CIDR 规则
     * @param clientIp 客户端 IP
     * @return 是否命中
     */
    public static boolean isMatchCidr(String cidr, String clientIp) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress networkAddress = parseIpLiteral(parts[0]);
            InetAddress currentAddress = parseIpLiteral(clientIp);
            byte[] networkBytes = networkAddress.getAddress();
            byte[] currentBytes = currentAddress.getAddress();
            if (networkBytes.length != currentBytes.length) {
                return false;
            }
            int prefixLength = Integer.parseInt(parts[1]);
            int maxPrefix = networkBytes.length * 8;
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                return false;
            }
            BigInteger mask = prefixLength == 0
                ? BigInteger.ZERO
                : BigInteger.ONE.shiftLeft(prefixLength).subtract(BigInteger.ONE).shiftLeft(maxPrefix - prefixLength);
            BigInteger network = new BigInteger(1, networkBytes);
            BigInteger current = new BigInteger(1, currentBytes);
            return network.and(mask).equals(current.and(mask));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析不含端口、括号、zone或主机名的IP字面量，不执行DNS查询。
     * IPv4仅接受四段十进制，IPv4映射IPv6统一为IPv4字节表示。
     *
     * @param value 数字IP字面量
     * @return 地址字节及规范文本
     * @throws IllegalArgumentException 输入不是明确的IP字面量
     */
    public static InetAddress parseIpLiteral(String value) {
        if (value == null || value.isEmpty() || value.length() > 45) {
            throw new IllegalArgumentException("Invalid IP literal");
        }
        if (value.indexOf(':') < 0) {
            validateIpv4(value);
        } else {
            // 带冒号且仅含这些字符的输入只可能进入JDK的数字IPv6解析器。
            if (!value.matches("[0-9A-Fa-f:.]+")) {
                throw new IllegalArgumentException("Invalid IP literal");
            }
            if (value.indexOf('.') >= 0) {
                validateIpv4(value.substring(value.lastIndexOf(':') + 1));
            }
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP literal");
        }
    }

    /**
     * 解析Servlet容器给出的socket peer；Jetty用方括号包围IPv6地址。
     * 此入口只用于getRemoteAddr，外来XFF仍使用严格字面量解析。
     *
     * @param peer Servlet容器提供的peer
     * @return 规范地址
     * @throws IllegalArgumentException 非数字地址或含端口等额外内容
     */
    public static InetAddress parseSocketPeer(String peer) {
        if (peer != null && peer.startsWith("[") && peer.endsWith("]") && peer.indexOf(':') >= 0) {
            return parseIpLiteral(peer.substring(1, peer.length() - 1));
        }
        return parseIpLiteral(peer);
    }

    private static void validateIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 literal");
        }
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) {
                throw new IllegalArgumentException("Invalid IPv4 literal");
            }
        }
    }

}
