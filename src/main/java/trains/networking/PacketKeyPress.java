package trains.networking;

import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import trains.TrainsInMotion;
import trains.entities.EntityTrainCore;
import trains.utility.EventHandler;

public class PacketKeyPress implements IMessage {
    private int key;
    /**
     * <h2>Packet for keybinds beyond inventory</h2>
     * when a key is pressed in
     * @see EventHandler#onClientKeyPress(InputEvent.KeyInputEvent)
     * it's processed by client to sent the defined action to server.
     * The only data sent in the packet, beyond the normal overhead is a single int to define what action to do.
     */
    public PacketKeyPress() {}
    public PacketKeyPress(int key) {
        this.key = key;
    }
    @Override
    public void fromBytes(ByteBuf bbuf) {
        key = bbuf.readInt();
    }
    @Override
    public void toBytes(ByteBuf bbuf) {
        bbuf.writeInt(key);
    }

    /**
     * <h2>packet handler</h2>
     * handles the packet when received by server
     * First it has to check if it was actually received by the proper entity, because if not, it crashes.
     * then check if the entity is correct and the key matches a valid key for a function.
     */
    public static class Handler implements IMessageHandler<PacketKeyPress, IMessage> {
        @Override
        public IMessage onMessage(PacketKeyPress message, MessageContext context) {
            Entity ridingEntity = context.getServerHandler().playerEntity.ridingEntity;
            //Toggles
            if (message.key == 0 && ridingEntity instanceof EntityTrainCore){
                ((EntityTrainCore) ridingEntity).lamp.isOn = !((EntityTrainCore) ridingEntity).lamp.isOn;
            }
            //speed
            else if (message.key == 2 && ridingEntity instanceof EntityTrainCore){
                ((EntityTrainCore) ridingEntity).setAcceleration(true);
            }else if (message.key == 3 && ridingEntity instanceof EntityTrainCore){
                ((EntityTrainCore) ridingEntity).setAcceleration(false);
            }
            /**
             * <h3>Manage the inventory key press</h3>
             * here we have to figure out what kind of train or rollingstock the player is riding, and activate the related GUI.
             */
            else if (message.key == 1){
                EntityPlayer entityPlayer = context.getServerHandler().playerEntity;
                if (entityPlayer != null && entityPlayer.ridingEntity instanceof EntityTrainCore) {
                    switch (((EntityTrainCore) entityPlayer.ridingEntity).getType()) {
                        case 1: {
                            entityPlayer.openGui(TrainsInMotion.instance, TrainsInMotion.STEAM_GUI_ID, entityPlayer.ridingEntity.worldObj,
                                    MathHelper.floor_double(entityPlayer.ridingEntity.posX), MathHelper.floor_double(entityPlayer.ridingEntity.posY),
                                    MathHelper.floor_double(entityPlayer.ridingEntity.posZ)); break;
                        }
                    }
                }
            }



            return null;
        }
    }
}
