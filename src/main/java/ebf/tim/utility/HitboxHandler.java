package ebf.tim.utility;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ebf.tim.TrainsInMotion;
import ebf.tim.entities.EntityBogie;
import ebf.tim.entities.EntityRollingStockCore;
import ebf.tim.entities.GenericRailTransport;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Collision and hitboxes</h1>
 * here we mnage collision detection and hitboxes, most of this is basically halfway between a minecart and the ender dragon.
 * @author Eternal Blue Flame
 */
public class HitboxHandler {
    public List<MultipartHitbox> hitboxList = new ArrayList<MultipartHitbox>();

    /**
     * <h2> basic hitbox class</h2>
     * a generic hitbox class used for adding multiple hitboxes to an entity.
     * requires the entity to override getParts() and extend IEntityMultiPart
     * The hitboxes also need to be moved every onUpdate, for trains this is handled in
     * @see HitboxHandler#getCollision(GenericRailTransport)
     */
    public class MultipartHitbox extends EntityDragonPart{
        public GenericRailTransport parent;
        public MultipartHitbox(IEntityMultiPart host, GenericRailTransport parent, double posX, double posY , double posZ){
            super(host, "hitboxGeneric", 2,1);
            this.parent = parent;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.setSize(1,2);
            this.boundingBox.minX = posX-0.45;
            this.boundingBox.minY = posY;
            this.boundingBox.minZ = posZ-0.45;
            this.boundingBox.maxX = posX+0.45;
            this.boundingBox.maxY = posY+2.5;
            this.boundingBox.maxZ = posZ+0.45;
        }
        @Override
        public AxisAlignedBB getBoundingBox(){
            return boundingBox;
        }
        @Override
        public AxisAlignedBB getCollisionBox(Entity collidedWith){
                return boundingBox;
        }
        @Override
        public void onUpdate(){
            if (parent == null|| worldObj.getEntityByID(parent.getEntityId()) == null){
                worldObj.removeEntity(this);
            }
        }
        @Override
        public boolean canBeCollidedWith() {
            return true;
        }
        @SideOnly(Side.CLIENT)
        public void setPositionAndRotation2(double p_70056_1_, double p_70056_3_, double p_70056_5_, float p_70056_7_, float p_70056_8_, int p_70056_9_) {
            this.setPosition(p_70056_1_, p_70056_3_, p_70056_5_);
            this.setRotation(p_70056_7_, p_70056_8_);
        }

    }


    /**
     * <h3>Process Entity Collision for train</h3>
     * this checks an area for blocks and other entities and returns true if there is something in that area besides what is supposed to be, (mounted players and bogies).
     */
    public boolean getCollision(GenericRailTransport transport){

        //Be sure the transport has hitboxes
        for (int iteration =0; iteration<transport.getHitboxPositions().length; iteration++) {
            double[] position = RailUtility.rotatePoint(new double[]{transport.getHitboxPositions()[iteration], 0, 0}, transport.rotationPitch, transport.rotationYaw, 0);
            if (hitboxList.size() <= iteration) {
                hitboxList.add(new MultipartHitbox(transport, transport, position[0] + transport.posX, position[1] + transport.posY, position[2] + transport.posZ));
                if (transport.worldObj.isRemote) {
                    transport.worldObj.spawnEntityInWorld(hitboxList.get(iteration));
                }
            } else {
                hitboxList.get(iteration).setLocationAndAngles(position[0] + transport.posX, position[1] + transport.posY, position[2] + transport.posZ, transport.rotationYaw, transport.rotationPitch);
            }
        }


        //detect collisions with blocks on X, Y, and Z.
        for (MultipartHitbox box : hitboxList){
            //define the values as temporary variables first since it will be faster than flooring the variable every loop check
            int i = MathHelper.floor_double(box.boundingBox.minX);
            int j = MathHelper.floor_double(box.boundingBox.minY);
            int k = MathHelper.floor_double(box.boundingBox.minZ);
            int l = MathHelper.floor_double(box.boundingBox.maxX);
            int i1 = MathHelper.floor_double(box.boundingBox.maxY);
            int j1 = MathHelper.floor_double(box.boundingBox.maxZ);
            /**
             * check if the chunk exists, then loop for X, Y, and Z to check for a rail or an air block.
             * if one isnt found return true to stop the movement.
             * @see GenericRailTransport#onUpdate()
             */
            if (transport.worldObj.checkChunksExist(i, j, k, l, i1, j1)) {
                for (int k1 = i; k1 <= l; ++k1) {
                    for (int l1 = j; l1 <= i1; ++l1) {
                        for (int i2 = k; i2 <= j1; ++i2) {
                            if (!(transport.worldObj.getBlock(k1, l1, i2) instanceof BlockAir) && !RailUtility.isRailBlockAt(transport.worldObj, k1, l1, i2)){
                                return !(transport.worldObj.getBlock(k1, l1+1, i2) instanceof BlockAir) && !RailUtility.isRailBlockAt(transport.worldObj, k1, l1+1, i2);
                            }
                        }
                    }
                }
            }


            /**
             * detect collision with entities.
             * we have to create a temporary bounding box that's larger than the current so we can check just a bit past the current box, this gives collision events a bit more time to react.
             * from there we create a list of entities in the new hitbox, and then loop through them to figure out what happens, we can't use the list directly due to concurrent modifications.
             */
            AxisAlignedBB tempBox = box.boundingBox.copy().expand(0.35,0,0.35);
            List list = transport.worldObj.getEntitiesWithinAABBExcludingEntity(transport, tempBox);
            if (list != null && list.size()>0) {
                for (Object entity: list) {
                    if (!(entity instanceof EntityBogie)) {
                        if (entity instanceof Entity) {
                            //if this is a rollingstock and it's a collision with a living entity like a player or a mob, decide if this should be pushed or if the entity should get run over.
                            if (transport instanceof EntityRollingStockCore && (entity instanceof EntityLiving || entity instanceof EntityPlayer)) {
                                if (transport.frontBogie.motionX > 0.5 || transport.frontBogie.motionX < -0.5 || transport.frontBogie.motionZ > 0.5 || transport.frontBogie.motionZ < -0.5) {
                                    //in the case of roadkill
                                    ((Entity) entity).attackEntityFrom(new EntityDamageSource("rollingstock", transport), (float) (transport.frontBogie.motionX + transport.frontBogie.motionZ) * 1000);
                                    ((Entity) entity).applyEntityCollision(transport);
                                    return false;
                                } else {
                                    //in the case of trying to move the rollingstock
                                    float directionZ = 0;
                                    float directionX = 0;
                                    if (((Entity) entity).posZ > transport.posZ + 0.5) {
                                        directionZ = -0.1f;
                                    } else if (((Entity) entity).posZ < transport.posZ - 0.5) {
                                        directionZ = 0.1f;
                                    }
                                    if (((Entity) entity).posX > transport.posX + 0.5) {
                                        directionX = 0.1f;
                                    } else if (((Entity) entity).posX < transport.posX - 0.5) {
                                        directionX = -0.1f;
                                    }
                                    double[] vec = RailUtility.rotatePoint(new double[]{directionZ, 0, directionX}, 0, Math.copySign(transport.rotationYaw, 1), 0);
                                    transport.addVelocity(vec[0], 0, vec[2]);
                                    ((Entity) entity).applyEntityCollision(transport);
                                    return false;
                                }

                                //if this is a train, just decide whether or not to run them over.
                            } else if (entity instanceof EntityLiving || entity instanceof EntityPlayer &&
                                    (transport.frontBogie.motionX > 0.5 || transport.frontBogie.motionX < -0.5 || transport.frontBogie.motionZ > 0.5 || transport.frontBogie.motionZ < -0.5)) {
                                ((Entity) entity).attackEntityFrom(new EntityDamageSource("train", transport), (float) (transport.frontBogie.motionX + transport.frontBogie.motionZ) * 1000);
                                ((Entity) entity).applyEntityCollision(transport);
                                return false;

                                //if it's a hitbox, return if it's one that we should collide with or not.
                                //we don't need to compensate for collisions on the other side because the other side is another instance of this.
                            } else if (entity instanceof MultipartHitbox) {
                                return (transport.frontLinkedTransport == ((MultipartHitbox) entity).parent.getPersistentID() ||
                                        transport.backLinkedTransport == ((MultipartHitbox) entity).parent.getPersistentID());

                                //if it's an item, then check if it's valid for the slot
                            } else if ((transport.getType() == TrainsInMotion.transportTypes.HOPPER || transport.getType() == TrainsInMotion.transportTypes.COALHOPPER ||
                                    transport.getType() == TrainsInMotion.transportTypes.GRAINHOPPER) &&
                                    entity instanceof EntityItem && transport.isItemValidForSlot(0,((EntityItem) entity).getEntityItem()) && ((EntityItem) entity).posY > transport.posY+1){
                                transport.addItem(((EntityItem) entity).getEntityItem());
                                ((EntityItem) entity).worldObj.removeEntity((EntityItem)entity);
                            }
                        }
                    }
                }
            }
        }
        //returning true stops the train, false lets it move.
        return false;
    }


}
