package org.namewta.common.nacos;

@FunctionalInterface
interface NacosConfigClientFactory {

    NacosConfigClient create(NacosConfigSettings settings) throws Exception;
}
