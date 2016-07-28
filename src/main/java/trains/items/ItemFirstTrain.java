package trains.items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import trains.TrainsInMotion;
import com.mojang.authlib.GameProfile;
import mods.railcraft.api.carts.IMinecart;
import mods.railcraft.api.core.items.IMinecartItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import trains.entities.MinecartExtended;
import trains.entities.trains.FirstTrain;
import trains.gui.GUITrain;

import java.util.UUID;

import static net.minecraft.util.MathHelper.floor_float;

public class ItemFirstTrain extends ItemMinecart implements IMinecart, IMinecartItem {
    //constructor
    public ItemFirstTrain() {
        super(0);
        setCreativeTab(TrainsInMotion.creativeTab);
    }
    /**
     *
     * @param owner the name of the player placing the cart or "[trainsinmotion]" with the brackets if it's being placed by a non-player
     * @param cart An ItemStack that contains the cart
     * @param world The World the cart is being placed in
     * @param posX the X position the cart is placed at
     * @param posY the Y position the cart is placed at
     * @param posZ the Z position the cart is placed at
     * @return
     */
    @Override
    public MinecartExtended placeCart(GameProfile owner, ItemStack cart, World world, int posX, int posY, int posZ) {
        /**
         * this will return anything that derrives MinecartExtended, in this case we use
         * @see FirstTrain#FirstTrain(UUID, World, double, double, double)
         *
         */
        return new FirstTrain(owner.getId(), world, posX,posY,posZ);
    }

    /**
     * spawns the train when the player/entity tries to use it on a tile.
     *
     * for information on the world spawn see
     * @see World#spawnEntityInWorld(Entity)
     *
     * for information on that the variables used in the spawn functions are doing
     * @see FirstTrain#FirstTrain(UUID, World, double, double, double)
     *
     * @param itemStack the itemstack that the cart comes from.
     * @param playerEntity the player entity using the item stack.
     * @param worldObj the world the item was used in
     * @param posX the X position that it was used on.
     * @param posY the Y position that it was used on.
     * @param posZ the Z position that it was used on.
     * @param blockSide the side of the block it was used on.
     * @param pointToRayX the X value of the ray trace to the exact position on the block it was used on.
     * @param pointToRayY the Y value of the ray trace to the exact position on the block it was used on.
     * @param pointToRayZ the Z value of the ray trace to the exact position on the block it was used on.
     *
     * @return defines whether or not to play the placing animation, we dont want to do this on server.
     */
    @Override
    public boolean onItemUse(ItemStack itemStack, EntityPlayer playerEntity, World worldObj, int posX, int posY, int posZ, int blockSide, float pointToRayX, float pointToRayY, float pointToRayZ) {
        if (!worldObj.isRemote) {
            MinecartExtended entity = new FirstTrain(playerEntity.getGameProfile().getId(), worldObj, posX,posY,posZ);
            Block block = worldObj.getBlock(posX,posY,posZ);

            if (BlockRailBase.func_150051_a(block)){
                if (((BlockRailBase)block).getBasicRailMetadata(worldObj, entity,posX,posY,posZ) == 0x0){

                    if (playerEntity.posZ > posZ){
                        entity.setDirection(0);
                    } else {
                        entity.setDirection(180);
                    }
                    worldObj.spawnEntityInWorld(entity);

                } else if (((BlockRailBase)block).getBasicRailMetadata(worldObj, entity,posX,posY,posZ) == 0x1){

                    if (playerEntity.posX >posX){
                        entity.setDirection(90);
                    } else {
                        entity.setDirection(270);
                    }
                    worldObj.spawnEntityInWorld(entity);

                } else {
                    playerEntity.addChatMessage(new ChatComponentText("You need to place on a straight piece of track."));
                    return false;
                }
            } else {
                return false;
            }
            return true;
        } else{
            return false;
        }
    }

    //if the item can be placed by a block or non-player entity
    @Override
    public boolean canBePlacedByNonPlayer(ItemStack cart) {
        return true;
    }
    //trains shouldn't match a cart filter.
    @Override
    public boolean doesCartMatchFilter(ItemStack stack, EntityMinecart cart) {
        return false;
    }

    /**
     * Sets the icon for the item
     */
    /*/
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon(Info.modID.toLowerCase() + ":trains/" + this.iconName);
    }
    /*/

}