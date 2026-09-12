package org.namewta.profile.person.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PersonMapperXmlTestSupport {

    public static void parse(Configuration configuration, Class<?> mapperType) {
        String resource = "mapper/person/" + mapperType.getSimpleName() + ".xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load mapper XML: " + resource, exception);
        }
    }
}
