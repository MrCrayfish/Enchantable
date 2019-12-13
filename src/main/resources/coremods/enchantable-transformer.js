function initializeCoreMod() {
	return {
		'left_click_event': {
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
                return classNode;
            }
        },
        'can_player_edit': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.entity.player.PlayerEntity'
            },
            'transformer': function(classNode) {
                log("Patching PlayerEntity...");
                patch({
                    obfName: "func_223729_a",
                    name: "",
                    desc: "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/GameType;)Z",
                    patch: patch_PlayerEntity_canPlayerEdit
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
    var foundNode = null;
    var instructions = method.instructions.toArray();
    var length = instructions.length;
    for (var i = 0; i < length; i++) {
        var node = instructions[i];
        if(node.getOpcode() != Opcodes.INVOKESTATIC)
            continue;
        if(!node.name.equals("onLeftClickBlock"))
            continue;
        if(node.getNext().getOpcode() != Opcodes.ASTORE)
            continue;
        foundNode = node.getNext();
        break;
    }
    if(foundNode !== null) {
        var label = new LabelNode();
        method.instructions.insert(foundNode, new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(foundNode, label);
        method.instructions.insert(foundNode, new InsnNode(Opcodes.IRETURN));
        method.instructions.insert(foundNode, new InsnNode(Opcodes.ICONST_0));
        method.instructions.insert(foundNode, new JumpInsnNode(Opcodes.IFNE, label));
        method.instructions.insert(foundNode, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/enchantable/Enchantable", "canLeftClickBlock", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;)Z", false));
        method.instructions.insert(foundNode, new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.insert(foundNode, new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.insert(foundNode, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", ASMAPI.mapField("field_71439_g"), "Lnet/minecraft/client/entity/player/ClientPlayerEntity;"));
        method.instructions.insert(foundNode, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/multiplayer/PlayerController", ASMAPI.mapField("field_78776_a"), "Lnet/minecraft/client/Minecraft;"));
        method.instructions.insert(foundNode, new VarInsnNode(Opcodes.ALOAD, 0));
        return true;
    }
    return false;
}

function patch_PlayerEntity_canPlayerEdit(method) {
    var label = new LabelNode();
    method.instructions.insert(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
    method.instructions.insert(label);
    method.instructions.insert(new InsnNode(Opcodes.IRETURN));
    method.instructions.insert(new InsnNode(Opcodes.ICONST_1));
    method.instructions.insert(new JumpInsnNode(Opcodes.IFNE, label));
    method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/enchantable/Enchantable", "canEditBlock", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z", false));
    method.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 2));
    method.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 1));
    method.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
    return true;
}

function getNthRelativeNode(node, n) {
    while(n > 0 && node !== null) {
        node = node.getNext();
        n--;
    }
    while(n < 0 && node !== null) {
        node = node.getPrevious();
        n++;
    }
    return node;
}

function log(s) {
    print("[enchantable-transformer.js] " + s);
}