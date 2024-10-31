@Environment(EnvType.CLIENT)
public class ClientRegionCopyMod implements ClientModInitializer {
    // ... previous code remains ...

    public static class SelectionManager {
        private BlockPos pointA;
        private BlockPos pointB;
        private static final int OUTLINE_COLOR = 0xFF00FF00; // Green color
        private static final int FILL_COLOR = 0x3300FF00; // Transparent green
        
        public void setPointA(BlockPos pos) {
            this.pointA = pos;
            MinecraftClient.getInstance().player.sendMessage(Text.literal("Point A set: " + pos.toShortString()));
        }
        
        public void setPointB(BlockPos pos) {
            this.pointB = pos;
            MinecraftClient.getInstance().player.sendMessage(Text.literal("Point B set: " + pos.toShortString()));
        }

        public void renderSelection(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera) {
            if (pointA == null || pointB == null) return;

            BlockPos min = new BlockPos(
                Math.min(pointA.getX(), pointB.getX()),
                Math.min(pointA.getY(), pointB.getY()),
                Math.min(pointA.getZ(), pointB.getZ())
            );
            
            BlockPos max = new BlockPos(
                Math.max(pointA.getX(), pointB.getX()) + 1,
                Math.max(pointA.getY(), pointB.getY()) + 1,
                Math.max(pointA.getZ(), pointB.getZ()) + 1
            );

            // Get camera position for proper rendering
            Vec3d cameraPos = camera.getPos();
            matrices.push();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            // Draw outline
            WorldRenderer.drawBox(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getLines()),
                Box.from(new Box(min, max)),
                ((OUTLINE_COLOR >> 16) & 0xFF) / 255.0f,
                ((OUTLINE_COLOR >> 8) & 0xFF) / 255.0f,
                (OUTLINE_COLOR & 0xFF) / 255.0f,
                ((OUTLINE_COLOR >> 24) & 0xFF) / 255.0f
            );

            // Draw filled box
            VertexConsumer fillBuffer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());
            drawFilledBox(
                matrices,
                fillBuffer,
                min,
                max,
                ((FILL_COLOR >> 16) & 0xFF) / 255.0f,
                ((FILL_COLOR >> 8) & 0xFF) / 255.0f,
                (FILL_COLOR & 0xFF) / 255.0f,
                ((FILL_COLOR >> 24) & 0xFF) / 255.0f
            );

            matrices.pop();
        }

        private void drawFilledBox(MatrixStack matrices, VertexConsumer buffer, BlockPos min, BlockPos max, float r, float g, float b, float a) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x1 = min.getX();
            float y1 = min.getY();
            float z1 = min.getZ();
            float x2 = max.getX();
            float y2 = max.getY();
            float z2 = max.getZ();

            // Bottom face
            buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();

            // Top face
            buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();

            // Front face
            buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();

            // Back face
            buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();

            // Left face
            buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();

            // Right face
            buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
            buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        }
    }

    @Override
    public void onInitializeClient() {
        // ... previous keybinding code remains ...

        // Register world render callback
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            selectionManager.renderSelection(
                context.matrixStack(),
                context.consumers(),
                context.camera()
            );
        });
    }

    // Add mixin to fix render order
    @Mixin(WorldRenderer.class)
    public static class WorldRendererMixin {
        @Inject(method = "render", at = @At("RETURN"))
        private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, 
                            Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, 
                            Matrix4f matrix4f, CallbackInfo ci) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            // Ensure selection renders through blocks
            RenderSystem.disableDepthTest();
            client.getBufferBuilders().getEntityVertexConsumers().draw();
            RenderSystem.enableDepthTest();
        }
    }
}
