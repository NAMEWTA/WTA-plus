package org.namewta.common.core.utils.ip;

import jakarta.servlet.http.HttpServletRequest;
import org.namewta.common.core.utils.NetUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/** 以socket peer和显式CIDR为信任边界的XFF解析器；配置不可变且默认不信任代理。 */
public final class ClientAddressResolver {
    private static final int MAX_HOPS = 32;
    private static final int MAX_HEADER_CHARS = 4096;
    private final List<Subnet> trustedProxies;

    /**
     * @param trustedProxyCidrs 可信代理CIDR；不接受主机名、通配符或隐式私网信任
     * @throws IllegalArgumentException 任一配置非法时拒绝启动
     */
    public ClientAddressResolver(List<String> trustedProxyCidrs) {
        trustedProxies = trustedProxyCidrs.stream().map(Subnet::parse).toList();
    }

    /**
     * 从右到左剥离可信代理；最右侧不可信地址是来源，左侧值不能改变它。
     *
     * @param request 尚未被容器转发头功能改写peer的请求
     * @return 规范IP地址
     * @throws IllegalArgumentException 可信peer提供畸形或超预算的XFF，或peer本身非法
     */
    public String resolve(HttpServletRequest request) {
        InetAddress peer = NetUtils.parseSocketPeer(request.getRemoteAddr());
        if (!trusted(peer)) {
            return peer.getHostAddress();
        }
        List<InetAddress> hops = new ArrayList<>();
        Enumeration<String> headers = request.getHeaders("X-Forwarded-For");
        int chars = 0;
        while (headers != null && headers.hasMoreElements()) {
            String header = headers.nextElement();
            chars += header.length();
            if (chars > MAX_HEADER_CHARS) {
                throw new IllegalArgumentException("Forwarded address chain is too long");
            }
            for (String part : header.split(",", -1)) {
                if (hops.size() == MAX_HOPS) {
                    throw new IllegalArgumentException("Too many forwarded addresses");
                }
                hops.add(NetUtils.parseIpLiteral(part.trim()));
            }
        }
        InetAddress address = peer;
        for (int i = hops.size() - 1; i >= 0 && trusted(address); i--) {
            address = hops.get(i);
        }
        return address.getHostAddress();
    }

    private boolean trusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(subnet -> subnet.contains(address.getAddress()));
    }

    private record Subnet(byte[] network, int prefix) {
        private static Subnet parse(String cidr) {
            if (cidr == null) {
                throw new IllegalArgumentException("Trusted proxy must be a numeric CIDR");
            }
            String[] parts = cidr.trim().split("/", -1);
            if (parts.length != 2 || !parts[1].matches("0|[1-9][0-9]{0,2}")) {
                throw new IllegalArgumentException("Trusted proxy must be a numeric CIDR");
            }
            byte[] network = NetUtils.parseIpLiteral(parts[0]).getAddress();
            int prefix = Integer.parseInt(parts[1]);
            if (prefix > network.length * 8) {
                throw new IllegalArgumentException("Invalid trusted proxy prefix");
            }
            return new Subnet(network, prefix);
        }

        private boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 1 << (7 - bit % 8);
                if ((network[bit / 8] & mask) != (address[bit / 8] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
