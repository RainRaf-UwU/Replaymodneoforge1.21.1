package de.johni0702.minecraft.gui.versions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashReportSection;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.render.VertexFormats;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static MinecraftClient getMinecraft() {
        return MinecraftClient.getInstance();
    }

    private static class ScissorBounds {
        private static final ScissorBounds DISABLED = new ScissorBounds(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private ScissorBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScissorBounds that = (ScissorBounds) o;
            return x == that.x && y == that.y && width == that.width && height == that.height;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, width, height);
        }
    }
    private static final ArrayDeque<ScissorBounds> scissorStateStack = new ArrayDeque<>();
    private static ScissorBounds scissorState = ScissorBounds.DISABLED;

    public static void pushScissorState() {
        scissorStateStack.push(scissorState);
    }

    public static void popScissorState() {
        setScissorBounds(scissorStateStack.pop());
    }

    public static void setScissorBounds(int x, int y, int width, int height) {
        setScissorBounds(new ScissorBounds(x, y, width, height));
    }

    public static void setScissorDisabled() {
        setScissorBounds(ScissorBounds.DISABLED);
    }

    private static void setScissorBounds(ScissorBounds newState) {
        ScissorBounds oldState = MCVer.scissorState;
        if (Objects.equals(oldState, newState)) {
            return;
        }

        scissorState = newState;

        boolean isEnabled = newState != ScissorBounds.DISABLED;
        boolean wasEnabled = oldState != ScissorBounds.DISABLED;

        if (isEnabled) {
            if (!wasEnabled) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            GL11.glScissor(scissorState.x, scissorState.y, scissorState.width, scissorState.height);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    public static Window newScaledResolution(MinecraftClient mc) {
        return mc.getWindow();
    }

    public static void addDetail(CrashReportSection category, String name, Callable<String> callable) {
        category.add(name, callable::call);
    }

    public static TextRenderer getFontRenderer() {
        return getMinecraft().textRenderer;
    }

    public static void setClipboardString(String text) {
        getMinecraft().keyboard.setClipboard(text);
    }

    public static String getClipboardString() {
        return getMinecraft().keyboard.getClipboard();
    }

    public static Text literalText(String str) {
        return Text.literal(str);
    }

    public static Identifier identifier(String id) {
        return Identifier.of(id);
    }

    public static Identifier identifier(String namespace, String path) {
        return Identifier.of(namespace, path);
    }

    public static abstract class Keyboard {
        public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
        public static final int KEY_HOME = GLFW.GLFW_KEY_HOME;
        public static final int KEY_END = GLFW.GLFW_KEY_END;
        public static final int KEY_UP = GLFW.GLFW_KEY_UP;
        public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
        public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
        public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
        public static final int KEY_BACK = GLFW.GLFW_KEY_BACKSPACE;
        public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;
        public static final int KEY_RETURN = GLFW.GLFW_KEY_ENTER;
        public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
        public static final int KEY_A = GLFW.GLFW_KEY_A;
        public static final int KEY_C = GLFW.GLFW_KEY_C;
        public static final int KEY_V = GLFW.GLFW_KEY_V;
        public static final int KEY_X = GLFW.GLFW_KEY_X;

        public static void enableRepeatEvents(boolean enabled) {
            // These are now always enabled and we no longer need to manually toggle it when opening screens
        }
    }
}
