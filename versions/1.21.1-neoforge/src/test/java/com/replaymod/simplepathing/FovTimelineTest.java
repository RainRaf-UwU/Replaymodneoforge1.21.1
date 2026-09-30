package com.replaymod.simplepathing;

import com.replaymod.pathing.properties.FovProperty;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class FovTimelineTest {
    private SPTimeline timeline;

    @Before public void setup() {
        ReplayModSimplePathing.LOGGER = LogManager.getLogger();
        timeline = new SPTimeline();
        timeline.setDefaultInterpolatorType(InterpolatorType.LINEAR);
    }

    private void add(long time, double fov) {
        timeline.addPositionKeyframe(time, 0, 0, 0, 0, 0, 0, -1, fov);
    }

    @Test public void interpolatesAndRestoresEditedFov() {
        add(0, 60);
        add(1000, 90);
        add(2000, 120);
        timeline.getPositionPath().update();
        assertEquals(75d, timeline.getPositionPath().getValue(FovProperty.PROPERTY, 500).get(), 0.001);
        Change change = timeline.updatePositionKeyframe(1000, 1, 2, 3, 4, 5, 6, 100);
        timeline.getTimeline().pushChange(change);
        assertEquals(100d, timeline.getPositionPath().getKeyframe(1000).getValue(FovProperty.PROPERTY).get(), 0);
        timeline.getTimeline().undoLastChange();
        assertEquals(90d, timeline.getPositionPath().getKeyframe(1000).getValue(FovProperty.PROPERTY).get(), 0);
        timeline.getTimeline().redoLastChange();
        assertEquals(100d, timeline.getPositionPath().getKeyframe(1000).getValue(FovProperty.PROPERTY).get(), 0);
        timeline.removePositionKeyframe(1000);
        timeline.getTimeline().undoLastChange();
        assertEquals(100d, timeline.getPositionPath().getKeyframe(1000).getValue(FovProperty.PROPERTY).get(), 0);
    }

    @Test public void savesAndLoadsFovAndAcceptsLegacyPaths() throws Exception {
        add(0, 60);
        add(1000, 100);
        TimelineSerialization serializer = new TimelineSerialization(timeline, null);
        String json = serializer.serialize(Collections.singletonMap("camera", timeline.getTimeline()));
        Timeline loaded = serializer.deserialize(json).get("camera");
        loaded.getPaths().get(1).update();
        assertEquals(80d, loaded.getPaths().get(1).getValue(FovProperty.PROPERTY, 500).get(), 0.001);

        SPTimeline legacy = new SPTimeline();
        legacy.setDefaultInterpolatorType(InterpolatorType.CUBIC);
        legacy.addPositionKeyframe(0, 0, 0, 0, 0, 0, 0, -1);
        legacy.addPositionKeyframe(1000, 1, 1, 1, 1, 1, 1, -1);
        String oldJson = serializer.serialize(Collections.singletonMap("legacy", legacy.getTimeline()));
        Timeline oldLoaded = serializer.deserialize(oldJson).get("legacy");
        oldLoaded.getPaths().get(1).update();
        assertFalse(oldLoaded.getPaths().get(1).getValue(FovProperty.PROPERTY, 500).isPresent());
    }

    @Test public void splineFovStaysPresentAcrossKeyframeMoves() {
        timeline.setDefaultInterpolatorType(InterpolatorType.CUBIC);
        add(0, 60);
        add(1000, 90);
        add(2000, 120);
        timeline.moveKeyframe(SPTimeline.SPPath.POSITION, 1000, 1200);
        timeline.getPositionPath().update();
        assertTrue(Double.isFinite(timeline.getPositionPath().getValue(FovProperty.PROPERTY, 500).get()));
        assertEquals(90d, timeline.getPositionPath().getKeyframe(1200).getValue(FovProperty.PROPERTY).get(), 0);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidFovBeforeAddingKeyframe() {
        add(0, 180);
    }
}
