package com.lulan.shincolle.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.path.ShipPathEntity;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEmotion;

/**ATTACK ON COLLIDE SHIP VERSION
 * host������@IShipAttack��IShipEmotion, �Bextend EntityCreature
 */
public class EntityAIShipAttackOnCollide extends EntityAIBase {
	
    World worldObj;
    IShipAttackBase host;
    EntityCreature host2;
    /** An amount of decrementing ticks that allows the entity to attack once the tick reaches 0. */
    int attackTick;
    /** The speed with which the mob will approach the target */
    double speedTowardsTarget;
    /** When true, the mob will continue chasing its target, even if it can't find a path to them right now. */
    boolean longMemory;
    /** The PathEntity of our entity. */
    ShipPathEntity entityPathEntity;
    Class classTarget;
    private int delayAttack;
    private double tarX;
    private double tarY;
    private double tarZ;

    private int failedPathFindingPenalty;

    public EntityAIShipAttackOnCollide(IShipAttackBase host, Class classTarget, double speed, boolean longMemory) {
        this(host, speed, longMemory);
        this.classTarget = classTarget;
    }

    public EntityAIShipAttackOnCollide(IShipAttackBase host, double speed, boolean longMemory) {
        this.host = host;
        this.host2 = (EntityCreature) host;
        this.worldObj = ((Entity)host).worldObj;
        this.speedTowardsTarget = speed;
        this.longMemory = longMemory;
        this.setMutexBits(3);
    }

    @Override
	public boolean shouldExecute() {
    	if(this.host2.isRiding()) {
    		return false;
    	}
    	
        EntityLivingBase entitylivingbase = this.host.getTarget();

        //�L�ؼ� or �ؼЦ��` or ���b���U�� ���Ұ�AI
        if(entitylivingbase == null || ((IShipEmotion)host).getIsSitting()) {
            return false;
        }
        else if(entitylivingbase != null && entitylivingbase.isDead) {
        	return false;
        }
        else if(this.classTarget != null && !this.classTarget.isAssignableFrom(entitylivingbase.getClass())) {
            return false;
        }
        else {
            if(-- this.delayAttack <= 0) {
                this.entityPathEntity = this.host.getShipNavigate().getPathToEntityLiving(entitylivingbase);
                this.delayAttack = 4 + this.host2.getRNG().nextInt(7);
                return this.entityPathEntity != null;
            }
            else {
                return true;
            }
        }
    }

    @Override
	public boolean continueExecuting() {
    	if(this.host2.isRiding()) {
    		return false;
    	}
    	
        EntityLivingBase entitylivingbase = this.host.getTarget();
        
        return (entitylivingbase == null || !entitylivingbase.isEntityAlive()) ? false : 
        	   (!this.longMemory ? !this.host.getShipNavigate().noPath() : 
        	   this.host2.isWithinHomeDistance(MathHelper.floor_double(entitylivingbase.posX), 
        			   							  MathHelper.floor_double(entitylivingbase.posY), 
        			   							  MathHelper.floor_double(entitylivingbase.posZ)));
    }

    @Override
	public void startExecuting() {
        this.host.getShipNavigate().setPath(this.entityPathEntity, speedTowardsTarget);
        this.delayAttack = 0;
    }

    @Override
	public void resetTask() {
        this.host.getShipNavigate().clearPathEntity();
        this.host2.setAttackTarget(null);
    }

    @Override
	public void updateTask() {
    	if(this.host2.isRiding()) {
    		return;
    	}
    	
        EntityLivingBase entitylivingbase = this.host.getTarget();
        
        //null check for target continue set null bug (set target -> clear target in one tick)
        if(entitylivingbase == null || entitylivingbase.isDead) {
        	resetTask();
        	return;
        }
        
        this.host2.getLookHelper().setLookPositionWithEntity(entitylivingbase, 30.0F, 30.0F);
        
        double distTarget = this.host2.getDistanceSq(entitylivingbase.posX, entitylivingbase.boundingBox.minY, entitylivingbase.posZ);
        double distAttack = this.host2.width * this.host2.width * 10F + entitylivingbase.width * 3F;
        
        --this.delayAttack;

        //�x�褺�ت���������AI
        if((this.longMemory || this.host2.getEntitySenses().canSee(entitylivingbase)) && this.delayAttack <= 0 && (this.tarX == 0.0D && this.tarY == 0.0D && this.tarZ == 0.0D || entitylivingbase.getDistanceSq(this.tarX, this.tarY, this.tarZ) >= 1.0D || this.host2.getRNG().nextFloat() < 0.1F)) {
            this.tarX = entitylivingbase.posX;
            this.tarY = entitylivingbase.boundingBox.minY;
            this.tarZ = entitylivingbase.posZ;
            this.delayAttack = failedPathFindingPenalty + 4 + this.host2.getRNG().nextInt(7);

            if(this.host.getShipNavigate().getPath() != null) {
                ShipPathPoint finalPathPoint = this.host.getShipNavigate().getPath().getFinalPathPoint();
                if(finalPathPoint != null && entitylivingbase.getDistanceSq(finalPathPoint.xCoord, finalPathPoint.yCoord, finalPathPoint.zCoord) < 1) {
                    failedPathFindingPenalty = 0;
                }
                else {
                    failedPathFindingPenalty += 10;
                }
            }
            else {
                failedPathFindingPenalty += 10;
            }

            if(distTarget > 1024.0D) {
                this.delayAttack += 10;
            }
            else if (distTarget > 256.0D) {
                this.delayAttack += 5;
            }

            if(!this.host.getShipNavigate().tryMoveToEntityLiving(entitylivingbase, speedTowardsTarget)) {
                this.delayAttack += 10;
            }
        }
        
//        //�b������, �ھڥؼЦ�m�W�U����
//        if(this.host2.isInWater()) {
////        	LogHelper.info("DEBUG : melee water move");
//        	double distY = this.tarY - this.host2.posY;
//        	
//        	if(distY > 1D) {
//        		this.host2.motionY = 0.15D;
//        	}
//        	else if(distY < -1D) {
//        		this.host2.motionY = -0.15D;
//        	}
//        	else {
//        		this.host2.motionY = 0D;
//        	}
//        	
//        	//�Y��������F��, �h���ո���
//    		if(this.host2.isCollidedHorizontally) {
//    			this.host2.motionY += 0.25D;
//    		}
//        }

        this.attackTick = Math.max(this.attackTick - 1, 0);

        if(distTarget <= distAttack && this.attackTick <= 20) {
            this.attackTick = 20;

            if(this.host2.getHeldItem() != null) {
                this.host2.swingItem();
            }

            this.host2.attackEntityAsMob(entitylivingbase);
        }
    }
}