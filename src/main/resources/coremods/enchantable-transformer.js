function initializeCoreMod() {
	return {
		'edit_block_event': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.multiplayer.PlayerController'
            },
            'transformer': function(classNode) {
                log("Patching PlayerController...");
                patch({
                    obfName: "func_180511_b",
                    name: "clickBlock",
                    desc: "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;)Z",
                    patch: patch_PlayerController_clickBlock
                }, classNode);
                patch({
                    obfName: "func_187103_a",
                    name: "onPlayerDestroyBlock",
                    desc: "(Lnet/minecraft/util/math/BlockPos;)Z",
                    patch: patch_PlayerController_onPlayerDestroyBlock
                }, classNode);
                patch({
                    obfName: "func_180512_c",
                    name: "onPlayerDamageBlock",
                    desc: "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;)Z",
                    patch: patch_PlayerController_onPlayerDamageBlock
                }, classNode);
                return classNode;
            }
        }
	};
}

function findMethod(methods, entry) {
    var length = methods.length;
    for(var i = 0; i < length; i++) {
        var method = methods[i];
        if((method.name.equals(entry.obfName) || method.name.equals(entry.name)) && method.desc.equals(entry.desc)) {
            return method;
        }
    }
    return null;
}

function patch(entry, classNode) {
    var method = findMethod(classNode.methods, entry);
    var name = classNode.name.replace("/", ".") + "#" + entry.name + entry.desc;
    if(method !== null) {
        log("Starting to patch: " + name);
        if(entry.patch(method)) {
            log("Successfully patched: " + name);
        } else {
            log("Failed to patch: " + name);
        }
    } else {
        log("Failed to find method: " + name);
    }
}

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var IntInsnNode = Java.type('org.objectweb.asm.tree.IntInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');

function patch_PlayerController_clickBlock(method) {
    insertEvent(method, method.instructions.get(0));
    return true;
}

function patch_PlayerController_onPlayerDestroyBlock(method) {
    insertEvent(method, method.instructions.get(0));
    return true;
}

function patch_PlayerController_onPlayerDamageBlock(method) {
    insertEvent(method, method.instructions.get(0));
    return true;
}

function insertEvent(method, firstInsn) {
    method.instructions.insertBefore(firstInsn, new VarInsnNode(Opcodes.ALOAD, 0));
    method.instructions.insertBefore(firstInsn, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/multiplayer/PlayerController", ASMAPI.mapField("field_78776_a"), "Lnet/minecraft/client/Minecraft;"));
    method.instructions.insertBefore(firstInsn, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", ASMAPI.mapField("field_71439_g"), "Lnet/minecraft/client/entity/player/ClientPlayerEntity;"));
    method.instructions.insertBefore(firstInsn, new VarInsnNode(Opcodes.ALOAD, 0));
    method.instructions.insertBefore(firstInsn, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/multiplayer/PlayerController", ASMAPI.mapField("field_78776_a"), "Lnet/minecraft/client/Minecraft;"));
    method.instructions.insertBefore(firstInsn, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", ASMAPI.mapField("field_71441_e"), "Lnet/minecraft/client/world/ClientWorld;"));
    method.instructions.insertBefore(firstInsn, new VarInsnNode(Opcodes.ALOAD, 1));
    method.instructions.insertBefore(firstInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/enchantable/Enchantable", "fireEditBlockEvent", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z", false));
    var label = new LabelNode();
    method.instructions.insertBefore(firstInsn, new JumpInsnNode(Opcodes.IFEQ, label));
    method.instructions.insertBefore(firstInsn, new InsnNode(Opcodes.ICONST_0));
    method.instructions.insertBefore(firstInsn, new InsnNode(Opcodes.IRETURN));
    method.instructions.insertBefore(firstInsn, label);
    method.instructions.insertBefore(firstInsn, new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
}

function log(s) {
    print("[enchantable-transformer.js] " + s);
}