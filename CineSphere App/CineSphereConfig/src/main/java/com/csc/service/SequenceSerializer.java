package com.csc.service;

import com.csc.model.EffectBlock;
import com.csc.model.EffectType;
import com.csc.model.TimelineModel;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Convierte la línea de tiempo al JSON que entiende el ESP32.
 *
 * <pre>
 * {"duration": 60.0,
 *  "events": [
 *    {"device":"FAN","dir":"LTR","start":1.0,"end":3.0,"value":75},
 *    {"device":"SERVO","start":2.0,"end":4.0,"value":120},
 *    {"device":"DIFFUSER","id":1,"start":0.5,"end":2.5}
 *  ]}
 * </pre>
 */
public final class SequenceSerializer {

    private SequenceSerializer() {}

    public static String toJson(TimelineModel model) {
        List<EffectBlock> blocks = model.getBlocks().stream()
                .sorted(Comparator.comparingDouble(EffectBlock::getStart))
                .toList();
        String events = blocks.stream()
                .map(b -> eventJson(b, b.getStart(), b.getEnd()))
                .collect(Collectors.joining(","));
        return String.format(Locale.ROOT, "{\"duration\":%.3f,\"events\":[%s]}",
                model.getTotalDuration(), events);
    }

    /** Un solo bloque, desplazado para que empiece en 0 (para probarlo al instante). */
    public static String testEventJson(EffectBlock block) {
        return eventJson(block, 0, block.getDuration());
    }

    private static String eventJson(EffectBlock b, double start, double end) {
        EffectType type = b.getType();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"device\":\"").append(type.getDevice()).append('"');
        switch (type) {
            case FAN_LEFT_TO_RIGHT -> sb.append(",\"dir\":\"LTR\"");
            case FAN_RIGHT_TO_LEFT -> sb.append(",\"dir\":\"RTL\"");
            case DIFFUSER_1 -> sb.append(",\"id\":1");
            case DIFFUSER_2 -> sb.append(",\"id\":2");
            case DIFFUSER_3 -> sb.append(",\"id\":3");
            case DIFFUSER_4 -> sb.append(",\"id\":4");
            default -> { }
        }
        sb.append(String.format(Locale.ROOT, ",\"start\":%.3f,\"end\":%.3f", start, end));
        if (type.hasParam()) sb.append(",\"value\":").append(Math.round(b.getValue()));
        return sb.append('}').toString();
    }
}
