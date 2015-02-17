package com.lulan.shincolle.entity;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**ENTITY ABYSS MISSILE
 * @parm world, host entity, tarX, tarY, tarZ, damage, knockback value
 * 
 * Parabola Orbit(for distance 7~65)
 * 形狀設定:
 * 在經過距離中點之前, 加上額外motionY向上以及accY向下
 * 到中點時, Vy = 0
 * 
 */
public class EntityAbyssMissile extends Entity {
	
    public BasicEntityShip hostEntity;  //host target
    public Entity hitEntity;			 //onImpact target (for entity)
    
    //missile motion
    public double distX;			//target distance
    public double distY;
    public double distZ;
    public boolean isDirect;		//false:parabola  true:direct
  
    //for parabola y position
    public double accParaY;			//額外y軸加速度
    public int midFlyTime;			//一半的飛行時間
    
    //for direct only
    public static final double ACCE = 0.02D;		//預設加速度
    public double accX;				//三軸加速度
    public double accY;
    public double accZ;
    
    //missile attributes
    public float atk;				//missile damage
    public float kbValue;			//knockback value
    public float missileHP;			//if hp = 0 -> onImpact
    public boolean isTargetHurt;	//knockback flag
    public World world;

    
    public EntityAbyssMissile(World world) {
    	super(world);
    }
    
    public EntityAbyssMissile(World world, BasicEntityShip host, double tarX, double tarY, double tarZ, double launchPos, float atk, float kbValue, boolean isDirect) {
        super(world);
        this.world = world;
        //設定entity的發射者, 用於追蹤造成傷害的來源
        this.hostEntity = host;
        this.setSize(1.0F, 1.0F);
        this.atk = atk;
        this.kbValue  = kbValue;
        //設定發射位置 (posY會加上offset), 左右+上下角度, 以及
        this.posX = host.posX;
        this.posY = launchPos;
        this.posZ = host.posZ;     
        //計算距離, 取得方向vector, 並且初始化速度, 使飛彈方向朝向目標
        this.distX = tarX - this.posX;
        this.distY = tarY - this.posY;
        this.distZ = tarZ - this.posZ;
        //設定直射或者拋物線
        this.isDirect = isDirect;
        
        //直射彈道, no gravity
    	double dist = (double)MathHelper.sqrt_double(this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ);
  	    this.accX = this.distX / dist * this.ACCE;
	    this.accY = this.distY / dist * this.ACCE;
	    this.accZ = this.distZ / dist * this.ACCE;
	    this.motionX = this.accX;
	    this.motionZ = this.accY;
	    this.motionY = this.accZ;
 
	    //拋物線軌道計算, y軸初速加上 (一半飛行時間 * 額外y軸加速度)
	    if(!this.isDirect) {
	    	this.midFlyTime = (int) (0.5D * MathHelper.sqrt_double(2D * dist / this.ACCE));
	    	this.accParaY = this.ACCE;
	    	this.motionY = this.motionY + (double)this.midFlyTime * this.accParaY;
	    }
    }

    protected void entityInit() {}

    /**
     * Checks if the entity is in range to render by using the past in distance and 
     * comparing it to its average bounding box edge length * 64 * renderDistanceWeight 
     * Args: distance
     * 
     * 由於entity可能不為正方體, 故取平均邊長大小來計算距離, 此方法預設為256倍邊長大小
     */
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        double d1 = this.boundingBox.getAverageEdgeLength() * 256D;
        return distanceSq < d1 * d1;
    }

    //update entity
    //注意: 移動要在server+client都做畫面才能顯示平順, particle則只能在client做
    public void onUpdate() {    	
    	/**********both side***********/
    	//將位置更新 (包含server, client間同步位置, 才能使bounding box運作正常)
        this.setPosition(this.posX, this.posY, this.posZ);
 
    	//計算發射體的高度
    	if(!this.isDirect) {  //直射軌道計算  	
			this.motionY = this.motionY + this.accY - this.accParaY;                   
    	}
    	else {
    		this.motionY += this.accY;
    	}
    	
    	//計算next tick的速度
        this.motionX += this.accX;
        this.motionZ += this.accZ;
        
    	//設定發射體的下一個位置
		this.posX += this.motionX;
		this.posY += this.motionY;
        this.posZ += this.motionZ;
           	
    	//計算模型要轉的角度 (RAD, not DEG)
        float f1 = MathHelper.sqrt_double(this.motionX*this.motionX + this.motionZ*this.motionZ);
        this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f1));
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ));    
        
        //依照x,z軸正負向修正角度(轉180)
        if(this.distX > 0) {
        	this.rotationYaw -= Math.PI;
        }
        else {
        	this.rotationYaw += Math.PI;
        }
        
        //更新位置等等基本資訊, 同時更新prePosXYZ
        super.onUpdate();
        
        /**********server side***********/
    	if(!this.worldObj.isRemote) {	
    		//發射超過20 sec, 設定為死亡(消失), 注意server restart後此值會歸零
    		if(this.ticksExisted > 600) {
    			this.setDead();	//直接抹消, 不觸發爆炸
    		}
    		
    		//該位置碰到方塊, 則設定爆炸 (方法1: 直接用座標找方塊) 此方法由於把座標取int, 很多時候看起來有撞到但是依然抓不到方塊
    		if(!this.worldObj.blockExists((int)this.posX, (int)this.posY, (int)this.posZ)) {
    			this.onImpact(null);
    		}
    		
    		//該位置碰到方塊, 則設定爆炸 (方法2: 用raytrace找方塊)
    		Vec3 vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            Vec3 vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            MovingObjectPosition movingobjectposition = this.worldObj.rayTraceBlocks(vec3, vec31);          
            vec3 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            vec31 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

            if (movingobjectposition != null) {
                vec31 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
                this.onImpact(null);
            }
    		
//            //debug
//            if(this.hostEntity != null) {
//            	LogHelper.info("DEBUG : tick "+this.ticksExisted);
//            	LogHelper.info("DEBUG : motionY "+this.motionY);
//            	LogHelper.info("DEBUG : mieeile rot: "+this.rotationPitch*180/Math.PI+" "+this.rotationYaw*180/Math.PI);
//            	LogHelper.info("DEBUG : host rot: "+this.hostEntity.rotationPitch+" "+this.hostEntity.rotationYaw);
//            	LogHelper.info("DEBUG : missile mot: "+this.motionX+" "+this.motionY+" "+this.motionZ);
//        		LogHelper.info("DEBUG : host pos: "+this.hostEntity.posX+" "+this.hostEntity.posY+" "+this.hostEntity.posZ); 
//            	LogHelper.info("DEBUG : tar pos: "+this.targetX+" "+this.targetY+" "+this.targetZ);
//            	LogHelper.info("DEBUG : diff pos: "+this.distX+" "+this.distY+" "+this.distZ);
//            	LogHelper.info("DEBUG : AABB: "+this.boundingBox.toString());
//            }
            
            //判定bounding box內是否有可以觸發爆炸的entity
            hitEntity = null;
            
            //在水中發射
            List hitList = null;
            if(this.hostEntity != null) {
            	if(this.hostEntity.getShipDepth() > 0) {
                	hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(1.0D, 1.0D, 1.0D));
                }
                else {	//在空氣中發射
                	hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(0.5D, 0.5D, 0.5D));
                }
            }
            else {	//for server restart, kill the missile entity
            	this.setDead();
            }
           
            //搜尋list, 找出第一個可以判定的目標, 即傳給onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) { 
                	hitEntity = (Entity)hitList.get(i);
                	if(hitEntity.canBeCollidedWith() && (!hitEntity.isEntityEqual(this.hostEntity) || this.ticksExisted > 30)) {               		
                		break;	//break for loop
                	}
                	else {
                		hitEntity = null;
                	}
                }
            }
            //call onImpact
            if(hitEntity != null) {
            	this.onImpact((EntityLivingBase)hitEntity);
            } 
            
    	}//end server side
    	/**********client side***********/
    	else {
    		//spawn particle
            for (int j = 0; j < 3; ++j) {
//                this.worldObj.spawnParticle("cloud", this.posX-this.motionX*1.5D*j, this.posY+1D-this.motionY*1.5D*j, this.posZ-this.motionZ*1.5D*j, -this.motionX*0.5D, -this.motionY*0.5D, -this.motionZ*0.5D);
                EntityFX particleSpray = new EntityFXSpray(worldObj, 
                		this.posX-this.motionX*1.5D*j, this.posY+1D-this.motionY*1.5D*j, this.posZ-this.motionZ*1.5D*j, 
                		-this.motionX*0.5D, -this.motionY*0.5D, -this.motionZ*0.5D,
                		1F, 1F, 1F, 1F);
            	Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray);
    		}
    	}//end client side
    	   	
    }

    //撞擊判定時呼叫此方法
    protected void onImpact(EntityLivingBase entityHit) {
    	//server side
    	if(!this.worldObj.isRemote) {  		
            if(entityHit != null) {	//撞到entity引起爆炸
            	//若攻擊到玩家, 傷害固定為TNT傷害
            	if(entityHit instanceof EntityPlayer) {
            		if(this.atk > 59) this.atk = 59;	//same with TNT
            	}
            	
        		//設定該entity受到的傷害
            	isTargetHurt = entityHit.attackEntityFrom(DamageSource.causeMobDamage(this.hostEntity), this.atk);

        	    //if attack success
        	    if(isTargetHurt) {
        	    	//calc kb effect
        	        if(this.kbValue > 0) {
        	        	entityHit.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
        	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
        	            motionX *= 0.6D;
        	            motionZ *= 0.6D;
        	        }             	 
        	    }
            }
            
            //計算範圍爆炸傷害: 判定bounding box內是否有可以吃傷害的entity
            hitEntity = null;
            AxisAlignedBB impactBox = this.boundingBox.expand(3.5D, 3.5D, 3.5D); 
            List hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
            //搜尋list, 找出第一個可以判定的目標, 即傳給onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) { 
                	hitEntity = (Entity)hitList.get(i);
                	if(hitEntity.canBeCollidedWith() && (!hitEntity.isEntityEqual(this.hostEntity) || this.ticksExisted > 20)) {               		
                		//若攻擊到玩家, 傷害固定為TNT傷害
                    	if(hitEntity instanceof EntityPlayer) {
                    		if(this.atk > 59) this.atk = 59;	//same with TNT
                    	}
                		
                		//對entity造成傷害
                		isTargetHurt = hitEntity.attackEntityFrom(DamageSource.causeMobDamage(this.hostEntity), this.atk);
                	    //if attack success
                	    if(isTargetHurt) {
                	    	//calc kb effect
                	        if(this.kbValue > 0) {
                	        	hitEntity.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
                	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
                	            motionX *= 0.6D;
                	            motionZ *= 0.6D;
                	        }             	 
                	    }
                	}
                }
            }          	

            
            //send packet to client for display partical effect
            TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
    		CommonProxy.channel.sendToAllAround(new S2CSpawnParticle(this, 2), point);
//            CreatePacketS2C.sendS2CAttackParticle(this, 2);
            this.setDead();
        }//end if server side
    }

    //儲存entity的nbt
    public void writeEntityToNBT(NBTTagCompound nbt) {
    	nbt.setTag("direction", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));  
    	nbt.setFloat("atk", this.atk);
    }

    //讀取entity的nbt
    public void readEntityFromNBT(NBTTagCompound nbt) {
        if(nbt.hasKey("direction", 9)) {	//9為tag list
            NBTTagList nbttaglist = nbt.getTagList("direction", 6);	//6為tag double
            this.motionX = nbttaglist.func_150309_d(0);	//此為get double
            this.motionY = nbttaglist.func_150309_d(1);
            this.motionZ = nbttaglist.func_150309_d(2);
        }
        else {
            this.setDead();
        }
        
        this.atk = nbt.getFloat("atk");
    }

    //設定true可使其他生物判定是否要閃開此entity
    public boolean canBeCollidedWith() {
        return true;
    }

    //取得此entity的bounding box大小
    public float getCollisionBorderSize() {
        return 1.0F;
    }

    //entity被攻擊到時呼叫此方法
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        if(this.isEntityInvulnerable()) {	//對無敵目標回傳false
            return false;
        }
        
        this.onImpact(null);
        return true;
    }

    //render用, 陰影大小
    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.0F;
    }

    //計算光線用
    public float getBrightness(float p_70013_1_) {
        return 1.0F;
    }

    //render用, 亮度值屬於亮紫色
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float p_70070_1_) {
        return 15728880;
    }
}
