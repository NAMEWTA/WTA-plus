package org.namewta.test.notify;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-JDK matcher for a single MyBatis updateById PreparedStatement.
 * No data source access, no SQL/value logging. An unrecognized SQL shape fails closed.
 */
final class OwnedAttachmentSendReservationSqlProbe {
    private static final Pattern ASSIGNMENT = Pattern.compile(
        "(?:^|,)\\s*send_reserved\\s*=\\s*\\?\\s*(?=,|$)");

    private final int sendReservedParameterIndex;
    private final Map<Integer, Object> bindings = new HashMap<>();
    private boolean batched;

    OwnedAttachmentSendReservationSqlProbe(String rawSql) {
        String sql = rawSql.replace("`", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        int set = sql.indexOf(" set ");
        int where = set < 0 ? -1 : sql.indexOf(" where ", set + 5);
        if (!sql.startsWith("update notify_intent_attachment ") || set < 0 || where < 0) {
            sendReservedParameterIndex = -1;
            return;
        }
        String assignments = sql.substring(set + 5, where);
        Matcher match = ASSIGNMENT.matcher(assignments);
        if (!match.find()) {
            sendReservedParameterIndex = -1;
            return;
        }
        int question = assignments.indexOf('?', match.start());
        if (question < 0 || question >= match.end() || match.find()) {
            sendReservedParameterIndex = -1;
            return;
        }
        int absoluteQuestion = set + 5 + question;
        sendReservedParameterIndex = countMarkers(sql, absoluteQuestion) + 1;
    }

    void bind(int position, Object value) {
        if (position > 0) bindings.put(position, value);
    }
    void clearParameters() { bindings.clear(); }
    void addBatch() { batched = true; }
    void clearBatch() { batched = false; }

    /** Exactly one affected relation row, with a bound true/one SET value. */
    boolean isSingleRowReservationUpdate(int affectedRows) {
        return !batched && affectedRows == 1 && sendReservedParameterIndex > 0
            && isTrue(bindings.get(sendReservedParameterIndex));
    }

    int sendReservedParameterIndexForOfflineTest() { return sendReservedParameterIndex; }

    private static boolean isTrue(Object value) {
        if (Boolean.TRUE.equals(value)) return true;
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue() == 1L;
        }
        return value instanceof BigInteger integer && BigInteger.ONE.equals(integer);
    }

    /** Count unquoted JDBC '?' bind markers strictly before one SET assignment. */
    private static int countMarkers(String sql, int endExclusive) {
        int count = 0;
        char quote = 0;
        for (int index = 0; index < endExclusive; index++) {
            char ch = sql.charAt(index);
            if (quote != 0) {
                if (ch == quote) {
                    if (index + 1 < endExclusive && sql.charAt(index + 1) == quote) index++;
                    else quote = 0;
                }
            } else if (ch == '\'' || ch == '"') {
                quote = ch;
            } else if (ch == '?') {
                count++;
            }
        }
        return count;
    }
}
