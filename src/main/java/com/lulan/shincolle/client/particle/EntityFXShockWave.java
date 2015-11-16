package com.lulan.shincolle.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


/**NYI
 */
@SideOnly(Side.CLIENT)
public class EntityFXShockWave extends EntityFX {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.TEXTURES_PARTICLE+"EntityFXShockWave.png");
	private Entity host;
	private int particleType;
	
    public EntityFXShockWave(World world, Entity host, float scale, int type) {
        super(world, host.posX+1.5D, host.posY+host.height*0.7D, host.posZ, 0.0D, 0.0D, 0.0D);  
//NYI
//        this.host = host;
//        this.motionX = 0D;
//        this.motionZ = 0D;
//        this.motionY = 0D;
//        this.particleScale = scale;
//        this.noClip = true;
//        this.particleType = type;
//        
//        switch(type) {
//        case 1:
//        	this.particleRed = 1F;
//        	this.particleGreen = 1F;
//        	this.particleBlue = 1F;
//        	this.particleAlpha = 1F;
//        	this.particleMaxAge = 50;
//        	break;
//        }
    }

    @Override
	public void renderParticle(Tessellator tess, float ticks, float par3, float par4, float par5, float par6, float par7) {       
		GL11.glPushMatrix();
		//bind texture
		Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		//particle�O�H���a������render, �]���y�Эn����interpPos�ഫ�����a�����y��
        double f11 = (float)(this.posX - interpPosX);
        double f12 = (float)(this.posY - interpPosY);
        double f13 = (float)(this.posZ - interpPosZ);
      
        //start tess
        tess.startDrawingQuads();
        //�`�N4���I�Φ������u�������|�K�W�K��, �Y���a�b�ӭ��I���|�ݤ��쥿���K��, �]���n�e�⭱�@8���I
        //�n�Ϫ��a�ݨ쥿��, 4�Ӯy��add���ǥ�����: �k�U -> �k�W -> ���W -> ���U
        //chi���Ϊ����K����, �`�@4�ӵ٧�6�ӳ��I
        //�Y�ӭ��_�I��y���C���I, ���|�e�{���W, �Y�_�I��������I, �ӭ��|�~�Y (�Y�|���I���A�P�@�����W��)
        //face1
        tess.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
        tess.setBrightness(240);
        tess.addVertexWithUV(f11, f12-particleScale, f13, 0D, 1D);
        tess.addVertexWithUV(f11+particleScale, f12, f13, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale, f13, 1D, 0D);
        tess.addVertexWithUV(f11, f12, f13+particleScale, 0D, 0D);
        //face2
        tess.addVertexWithUV(f11, f12-particleScale, f13, 0D, 1D);
        tess.addVertexWithUV(f11, f12, f13-particleScale, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale, f13, 1D, 0D);
        tess.addVertexWithUV(f11+particleScale, f12, f13, 0D, 0D);
        //face3
        tess.addVertexWithUV(f11, f12-particleScale, f13, 0D, 1D);
        tess.addVertexWithUV(f11-particleScale, f12, f13, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale, f13, 1D, 0D);
        tess.addVertexWithUV(f11, f12, f13-particleScale, 0D, 0D);
        //face4
        tess.addVertexWithUV(f11, f12-particleScale, f13, 0D, 1D);
        tess.addVertexWithUV(f11, f12, f13+particleScale, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale, f13, 1D, 0D);
        tess.addVertexWithUV(f11-particleScale, f12, f13, 0D, 0D);
        
        //�b�z���~��
        //face1
        tess.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha * 0.5F);
        tess.addVertexWithUV(f11, f12-particleScale*1.3, f13, 0D, 1D);
        tess.addVertexWithUV(f11+particleScale*1.3, f12, f13, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale*1.3, f13, 1D, 0D);
        tess.addVertexWithUV(f11, f12, f13+particleScale*1.3, 0D, 0D);
        //face2
        tess.addVertexWithUV(f11, f12-particleScale*1.3, f13, 0D, 1D);
        tess.addVertexWithUV(f11, f12, f13-particleScale*1.3, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale*1.3, f13, 1D, 0D);
        tess.addVertexWithUV(f11+particleScale*1.3, f12, f13, 0D, 0D);
        //face3
        tess.addVertexWithUV(f11, f12-particleScale*1.3, f13, 0D, 1D);
        tess.addVertexWithUV(f11-particleScale*1.3, f12, f13, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale*1.3, f13, 1D, 0D);
        tess.addVertexWithUV(f11, f12, f13-particleScale*1.3, 0D, 0D);
        //face4
        tess.addVertexWithUV(f11, f12-particleScale*1.3, f13, 0D, 1D);
        tess.addVertexWithUV(f11, f12, f13+particleScale*1.3, 1D, 1D);
        tess.addVertexWithUV(f11, f12+particleScale*1.3, f13, 1D, 0D);
        tess.addVertexWithUV(f11-particleScale*1.3, f12, f13, 0D, 0D);
        
        //stop tess for restore texture
        tess.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(false);
		GL11.glPopMatrix();
    }
    
    //layer: 0:particle 1:terrain 2:items 3:custom?
    @Override
    public int getFXLayer() {
        return 3;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
	public void onUpdate() {
    	//this is both side particle
		this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        
        float[] newPos = ParticleHelper.rotateXZByAxis(1F, 0F, 6.28F / this.particleMaxAge * this.particleAge, 1F);

        if(this.host != null) {
        	this.posX = this.host.posX + newPos[0];
            this.posZ = this.host.posZ + newPos[1];
        }
        
        int phase = ((IShipEmotion)host).getStateEmotion(ID.S.Phase);
        
        if(this.particleAge++ > this.particleMaxAge) {
            this.setDead();
        }
        else if(phase == 0 || phase == 2) {
        	this.setDead();
        }
        
    }
}

