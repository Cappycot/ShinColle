package com.lulan.shincolle.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**SERVER TO CLIENT : SPAWN PARTICLE PACKET
 * 用於指定位置生成particle
 * packet handler同樣建立在此class中
 * 
 * tut by diesieben07: http://www.minecraftforge.net/forum/index.php/topic,20135.0.html
 */
public class S2CSpawnParticle implements IMessage {
	
	private Entity sendEntity;
	private Entity recvEntity;
	private int entityID;
	private int sendType;
	private int recvType;
	private byte sendParticleType;
	private byte recvParticleType;
	private boolean sendIsShip;
	private boolean recvIsShip;
	private float sendposX;
	private float sendposY;
	private float sendposZ;
	private float sendlookX;
	private float sendlookY;
	private float sendlookZ;
	private float recvposX;
	private float recvposY;
	private float recvposZ;
	private float recvlookX;
	private float recvlookY;
	private float recvlookZ;
	
	
	public S2CSpawnParticle() {}	//必須要有空參數constructor, forge才能使用此class
	
	//spawn particle: 
	//type 0: spawn particle with entity, if isShip -> set ship attackTime
	public S2CSpawnParticle(Entity entity, int type, boolean isShip) {
        this.sendEntity = entity;
        this.sendType = 0;
        this.sendIsShip = isShip;
        this.sendParticleType = (byte)type;
    }
	
	//type 1: spawn particle with entity and position, if isShip -> set ship attackTime
	public S2CSpawnParticle(Entity entity, int type, double posX, double posY, double posZ, double lookX, double lookY, double lookZ, boolean isShip) {
		this.sendEntity = entity;
        this.sendType = 1;
        this.sendIsShip = isShip;
        this.sendParticleType = (byte)type;
        this.sendposX = (float)posX;
        this.sendposY = (float)posY;
        this.sendposZ = (float)posZ;
        this.sendlookX = (float)lookX;
        this.sendlookY = (float)lookY;
        this.sendlookZ = (float)lookZ;
    }
	
	//type 2: spawn particle with position
	public S2CSpawnParticle(int type, double posX, double posY, double posZ, double lookX, double lookY, double lookZ) {
        this.sendType = 2;
        this.sendParticleType = (byte)type;
        this.sendposX = (float)posX;
        this.sendposY = (float)posY;
        this.sendposZ = (float)posZ;
        this.sendlookX = (float)lookX;
        this.sendlookY = (float)lookY;
        this.sendlookZ = (float)lookZ;
    }
	
	//接收packet方法
	@Override
	public void fromBytes(ByteBuf buf) {
		World clientWorld = ClientProxy.getClientWorld();
		//get type and entityID
		this.recvType = buf.readByte();
	
		switch(recvType) {
		case 0:	//spawn particle with entity
			{
				this.entityID = buf.readInt();
				recvEntity = EntityHelper.getEntityByID(entityID, clientWorld);
				this.recvParticleType = buf.readByte();
				this.recvIsShip = buf.readBoolean();
				ParticleHelper.spawnAttackParticle(recvEntity, recvParticleType, recvIsShip);	
			}
			break;
		case 1: //spawn particle with entity and position
			{
				this.entityID = buf.readInt();
				recvEntity = EntityHelper.getEntityByID(entityID, clientWorld);
				this.recvParticleType = buf.readByte();
				this.recvIsShip = buf.readBoolean();
				this.recvposX = buf.readFloat();
				this.recvposY = buf.readFloat();
				this.recvposZ = buf.readFloat();
				this.recvlookX = buf.readFloat();
				this.recvlookY = buf.readFloat();
				this.recvlookZ = buf.readFloat();
				ParticleHelper.spawnAttackParticleCustomVector(recvEntity, (double)recvposX, (double)recvposY, (double)recvposZ, (double)recvlookX, (double)recvlookY, (double)recvlookZ, recvParticleType, recvIsShip);
			}
			break;
		case 2: //spawn particle with position
			{
				this.recvParticleType = buf.readByte();
				this.recvposX = buf.readFloat();
				this.recvposY = buf.readFloat();
				this.recvposZ = buf.readFloat();
				this.recvlookX = buf.readFloat();
				this.recvlookY = buf.readFloat();
				this.recvlookZ = buf.readFloat();
				ParticleHelper.spawnAttackParticleAt(recvposX, recvposY, recvposZ, recvlookX, recvlookY, recvlookZ, recvParticleType);
			}
			break;
		}
	}

	//發出packet方法
	@Override
	public void toBytes(ByteBuf buf) {
		switch(this.sendType) {
		case 0:	//spawn particle with entity
			{
				buf.writeByte(0);	//type 0
				buf.writeInt(this.sendEntity.getEntityId());
				buf.writeByte(this.sendParticleType);
				buf.writeBoolean(this.sendIsShip);
			}
			break;
		case 1:	//spawn particle with entity and position
			{
				buf.writeByte(1);	//type 1
				buf.writeInt(this.sendEntity.getEntityId());
				buf.writeByte(this.sendParticleType);
				buf.writeBoolean(this.sendIsShip);
				buf.writeFloat(this.sendposX);
				buf.writeFloat(this.sendposY);
				buf.writeFloat(this.sendposZ);
				buf.writeFloat(this.sendlookX);
				buf.writeFloat(this.sendlookY);
				buf.writeFloat(this.sendlookZ);
			}
			break;
		case 2:	//spawn particle with position
			{
				buf.writeByte(2);	//type 2
				buf.writeByte(this.sendParticleType);
				buf.writeFloat(this.sendposX);
				buf.writeFloat(this.sendposY);
				buf.writeFloat(this.sendposZ);
				buf.writeFloat(this.sendlookX);
				buf.writeFloat(this.sendlookY);
				buf.writeFloat(this.sendlookZ);
			}
			break;
		}
	}
	
	//packet handler (inner class)
	public static class Handler implements IMessageHandler<S2CSpawnParticle, IMessage> {
		//收到封包時顯示debug訊息
		@Override
		public IMessage onMessage(S2CSpawnParticle message, MessageContext ctx) {
//          System.out.println(String.format("Received %s from %s", message.text, ctx.getServerHandler().playerEntity.getDisplayName()));
//			LogHelper.info("DEBUG : recv Spawn Particle packet : type "+recvType+" particle "+recvParticleType);
			return null;
		}
    }
	

}

