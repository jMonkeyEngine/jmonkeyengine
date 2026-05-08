package com.jme3.vulkan.spvc;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spvc.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SpvcCompiler implements CompiledShaderCode {

    public enum Backend {

        None(Spvc.SPVC_BACKEND_NONE),
        Glsl(Spvc.SPVC_BACKEND_GLSL),
        Hlsl(Spvc.SPVC_BACKEND_HLSL),
        Cpp(Spvc.SPVC_BACKEND_CPP),
        Json(Spvc.SPVC_BACKEND_JSON),
        Msl(Spvc.SPVC_BACKEND_MSL);

        private final int backend;

        Backend(int backend) {
            this.backend = backend;
        }

        public int getBackend() {
            return backend;
        }

    }

    public enum Decoration {

        DescriptorSet(Spv.SpvDecorationDescriptorSet),
        Binding(Spv.SpvDecorationBinding),
        Location(Spv.SpvDecorationLocation);

        private final int decoration;

        Decoration(int decoration) {
            this.decoration = decoration;
        }

        public int getDecoration() {
            return decoration;
        }

    }

    public enum ResourceType {

        AccelerationStructure(Spvc.SPVC_RESOURCE_TYPE_ACCELERATION_STRUCTURE),
        UniformBuffer(Spvc.SPVC_RESOURCE_TYPE_UNIFORM_BUFFER),
        StorageBuffer(Spvc.SPVC_RESOURCE_TYPE_STORAGE_BUFFER);

        private final int type;

        ResourceType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public static ResourceType of(int type) {
            for (ResourceType t : values()) {
                if (t.getType() == type) return t;
            }
            return null;
        }

    }

    public enum BaseType {

        AccelerationStructure(Spvc.SPVC_BASETYPE_ACCELERATION_STRUCTURE),
        Struct(Spvc.SPVC_BASETYPE_STRUCT),
        Float64(Spvc.SPVC_BASETYPE_FP64),
        Float32(Spvc.SPVC_BASETYPE_FP32),
        Float16(Spvc.SPVC_BASETYPE_FP16),
        Int64(Spvc.SPVC_BASETYPE_INT64),
        Int32(Spvc.SPVC_BASETYPE_INT32),
        Int16(Spvc.SPVC_BASETYPE_INT16),
        Int8(Spvc.SPVC_BASETYPE_INT8),
        UInt64(Spvc.SPVC_BASETYPE_UINT64),
        UInt32(Spvc.SPVC_BASETYPE_UINT32),
        UInt16(Spvc.SPVC_BASETYPE_UINT16),
        UInt8(Spvc.SPVC_BASETYPE_UINT8),
        Boolean(Spvc.SPVC_BASETYPE_BOOLEAN),
        Image(Spvc.SPVC_BASETYPE_IMAGE),
        SampledImage(Spvc.SPVC_BASETYPE_SAMPLED_IMAGE),
        Sampler(Spvc.SPVC_BASETYPE_SAMPLER);

        private final int type;

        BaseType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public static BaseType of(int type) {
            for (BaseType t : values()) {
                if (t.getType() == type) return t;
            }
            return null;
        }

    }

    private final long compiler;
    private final ByteBuffer code;
    private long resourcesHandle = MemoryUtil.NULL;
    private Collection<EntryPoint> entryPoints;
    private final Map<ResourceType, Collection<ShaderResource>> resourcesByType = new HashMap<>();

    public SpvcCompiler(SpvcContext context, Backend target, ByteBuffer spirv) {
        this.code = spirv;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer parsed = stack.mallocPointer(1);
            Spvc.spvc_context_parse_spirv(context.get(), spirv.asIntBuffer(), spirv.remaining() / 4, parsed);
            PointerBuffer comp = stack.mallocPointer(1);
            Spvc.spvc_context_create_compiler(context.get(), target.getBackend(), parsed.get(0), Spvc.SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, comp);
            compiler = comp.get(0);
        }
    }

    @Override
    public Collection<ShaderResource> getResourcesForType(ResourceType type) {
        Collection<ShaderResource> resourceList = resourcesByType.get(type);
        if (resourceList == null) try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer address = stack.mallocPointer(1);
            if (resourcesHandle == MemoryUtil.NULL) {
                Spvc.spvc_compiler_create_shader_resources(compiler, address);
                resourcesHandle = address.get(0);
            }
            PointerBuffer count = stack.mallocPointer(1);
            Spvc.spvc_resources_get_resource_list_for_type(resourcesHandle, type.getType(), address, count);
            SpvcReflectedResource.Buffer resBuf = SpvcReflectedResource.create(address.get(0), (int) count.get(0));
            resourceList = new ArrayList<>(resBuf.remaining());
            for (SpvcReflectedResource r : resBuf) {
                resourceList.add(new ReflectedResource(r));
            }
            resourcesByType.put(type, resourceList);
        }
        return null;
    }

    @Override
    public Collection<EntryPoint> getEntryPoints() {
        if (entryPoints == null) try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer listAddress = stack.mallocPointer(1);
            PointerBuffer count = stack.mallocPointer(1);
            Spvc.spvc_compiler_get_entry_points(compiler, listAddress, count);
            SpvcEntryPoint.Buffer epBuf = SpvcEntryPoint.create(listAddress.get(0), (int)count.get(0));
            entryPoints = new ArrayList<>(epBuf.remaining());
            for (SpvcEntryPoint p : epBuf) {
                entryPoints.add(new EntryPoint(p));
            }
        }
        return entryPoints;
    }

    public String compile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.mallocPointer(1);
            Spvc.spvc_compiler_compile(compiler, out);
            return out.getStringUTF8();
        }
    }

    public long get() {
        return compiler;
    }

    @Override
    public ByteBuffer getCompiledCode() {
        return code.asReadOnlyBuffer();
    }

    private class ReflectedResource implements ShaderResource {

        private final SpvcReflectedResource resource;

        public ReflectedResource(SpvcReflectedResource resource) {
            this.resource = resource;
        }

        @Override
        public String getName() {
            return resource.nameString();
        }

        @Override
        public BaseType getBaseType() {
            return BaseType.of(resource.base_type_id());
        }

        @Override
        public ResourceType getType() {
            return ResourceType.of(resource.type_id());
        }

        @Override
        public int getDecoration(SpvcCompiler.Decoration type) {
            return Spvc.spvc_compiler_get_decoration(compiler, resource.id(), type.getDecoration());
        }

    }

}
