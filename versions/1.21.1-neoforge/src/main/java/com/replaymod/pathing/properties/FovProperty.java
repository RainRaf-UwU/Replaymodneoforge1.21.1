package com.replaymod.pathing.properties;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replaystudio.pathing.property.AbstractProperty;
import com.replaymod.replaystudio.pathing.property.AbstractPropertyPart;
import com.replaymod.replaystudio.pathing.property.PropertyPart;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/** Vertical field of view in degrees, serialized alongside position and rotation. */
public final class FovProperty extends AbstractProperty<Double> {
    public static final FovProperty PROPERTY = new FovProperty();
    private final PropertyPart<Double> part = new AbstractPropertyPart<Double>(this, true) {
        @Override public double toDouble(Double value) { return value; }
        @Override public Double fromDouble(Double previous, double value) { return value; }
    };

    private FovProperty() {
        super("fov", "options.fov", CameraProperties.GROUP, 70d);
    }

    @Override public Collection<PropertyPart<Double>> getParts() {
        return Collections.singletonList(part);
    }

    @Override public void applyToGame(Double value, Object replayHandler) {
        ReplayHandler handler = (ReplayHandler) replayHandler;
        // Spectator keyframes keep the spectated entity's ordinary FOV.
        if (!handler.isCameraView()) return;
        CameraEntity camera = handler.getCameraEntity();
        if (camera != null && Double.isFinite(value)) {
            // Spline interpolation may overshoot its control points.
            camera.setCameraFov(Math.max(1d, Math.min(179d, value)));
        }
    }

    @Override public void toJson(JsonWriter writer, Double value) throws IOException {
        validate(value);
        writer.value(value);
    }

    @Override public Double fromJson(JsonReader reader) throws IOException {
        double value = reader.nextDouble();
        validate(value);
        return value;
    }

    private static void validate(double value) throws IOException {
        if (!Double.isFinite(value) || value < 1d || value > 179d) {
            throw new IOException("Camera FOV must be between 1 and 179 degrees: " + value);
        }
    }
}
