import org.namewta.common.json.utils.JsonUtils;
import tools.jackson.databind.JsonNode;

/** Zero-network Jackson 3 node operation check; only exception class names leave stdout. */
public final class T45JacksonNodeProbe {
    public static void main(String[] args) throws Exception {
        JsonNode object = JsonUtils.getJsonMapper().readTree("{\"AWS\":[\"*\"]}");
        JsonNode array = object.get("AWS");
        show("object_asText", object);
        show("array_asText", array);
        show("element_asText", array.get(0));
    }

    private static void show(String label, JsonNode node) {
        try {
            node.asText();
            System.out.println(label + "=OK");
        } catch (Exception failure) {
            System.out.println(label + "=" + failure.getClass().getSimpleName());
        }
    }
}
