package tk.greydynamics.Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import tk.greydynamics.Resource.FileHandler;
import tk.greydynamics.Resource.Frostbite3.ITEXTURE.ImageConverter;
import tk.greydynamics.Resource.Frostbite3.ITEXTURE.ImageConverter.ImageType;

public class Loader {
	private ArrayList<Integer> vaos = new ArrayList<Integer>();
	private ArrayList<Integer> vbos = new ArrayList<Integer>();
	private HashMap<String, Integer> textures = new HashMap<String, Integer>();
	private int notFoundID;
	private int crosshairID;
	
	public RawModel loadVAO(String name, int drawMethod, float[] positions, float[] uvs, int[] indices){
		int vaoID = createVAO();
		bindIndiciesBuffer(indices);
		storeDataAsAttr(0, 3, positions);
		storeDataAsAttr(1, 2, uvs);
		unbindVAO();
		return new RawModel(name, vaoID, indices.length, drawMethod);
	}
	
	public RawModel loadVAO(String name, int drawMethod, float[] positions, float[] uvs, float[] normals, int[] indices){
		int vaoID = createVAO();
		bindIndiciesBuffer(indices);
		storeDataAsAttr(0, 3, positions);
		storeDataAsAttr(1, 2, uvs);
		storeDataAsAttr(2, 4, normals);
		unbindVAO();
		return new RawModel(name, vaoID, indices.length, drawMethod);
	}
	
	public RawModel loadVAO(String name, int drawMethod, float[] positions, int[] indices){
		int vaoID = createVAO();
		bindIndiciesBuffer(indices);
		storeDataAsAttr(0, 3, positions);
		unbindVAO();
		return new RawModel(name, vaoID, indices.length, drawMethod);
	}
	
	public RawModel loadVAO(String name, int drawMethod, float[] positions){
		int vaoID = createVAO();
		storeDataInAttributeList(0, 2, positions);
		unbindVAO();
		return new RawModel(name, vaoID, positions.length/2, drawMethod);
	}	
	
	public int createVAO(){
		int vaoID = GL30.glGenVertexArrays();
		vaos.add(vaoID);
		GL30.glBindVertexArray(vaoID);
		return vaoID;
	}
	
	public void cleanUp(){
		for(int vao: vaos){
			cleanVAO(vao, false);
		}
		vaos.clear();
		for(int vbo: vbos){
			cleanVBO(vbo, false);
		}
		vbos.clear();
		for(int texture: textures.values()){
			cleanTexture(texture, false);
		}
		textures.clear();
	}
	
	public void cleanVAO(int vaoID, boolean deleteFromList){
		GL30.glDeleteVertexArrays(vaoID);
		if (deleteFromList){
			this.vaos.remove((Object) vaoID);
		}
	}
	public void cleanVBO(int vboID, boolean deleteFromList){
		GL15.glDeleteBuffers(vboID);
		if (deleteFromList){
			this.vbos.remove((Object) vboID);
		}
	}
	public void cleanTexture(int textureID, boolean deleteFromList){
		GL11.glDeleteTextures(textureID);
		if (deleteFromList){
			this.textures.remove((Object) textureID);
		}
	}
	
	public Loader(){
		notFoundID = 0;
		crosshairID = 0;
	}
	public void init(){
		notFoundID = loadTexture("res/notFound/notFound.dds");
		crosshairID = loadTexture("res/interface/crosshair-vector.png");
	}
	
	public int getNotFoundID() {
		return notFoundID;
	}
	

	public int getCrosshairID() {
		return crosshairID;
	}
	
	public RawModel loadOBJSimple(String f, int drawMethod){
		try{
	        BufferedReader reader = new BufferedReader(new FileReader(new File(f)));
	        String line;
	        String name = null;
	        ArrayList<Float> vertices = new ArrayList<>();
	        ArrayList<Float> normals = new ArrayList<>();
	        ArrayList<Float> uvs = new ArrayList<>();
	        ArrayList<Integer> indices = new ArrayList<>();
	        while ((line = reader.readLine()) != null) {
	            if (line.startsWith("#")) {
	                continue;
	            }
	            if (line.startsWith("o ")) {
	            	//o Axis_Base
	            	String[] words = line.split(" ");
	            	name = words[1];
	            	System.out.println("[OBJ] Loading "+name);
	            } else if (line.startsWith("v ")) {
	            	//v 0.020400 0.542908 -0.053202
	                String[] xyz = line.split(" ");
	                vertices.add(Float.valueOf(xyz[1]));
	                vertices.add(Float.valueOf(xyz[2]));
	                vertices.add(Float.valueOf(xyz[3]));
	            } else if (line.startsWith("vn ")) {
	            	//v 0.0 1.0 0.0
	                String[] xyz = line.split(" ");
	                normals.add(Float.valueOf(xyz[1]));
	                normals.add(Float.valueOf(xyz[2]));
	                normals.add(Float.valueOf(xyz[3]));
	            } else if (line.startsWith("vt ")) {
	            	//v 0.0 0.0
	                String[] xyz = line.split(" ");
	                uvs.add(Float.valueOf(xyz[1]));
	                uvs.add(Float.valueOf(xyz[2]));
	            } else if (line.startsWith("f ")) {
	            	//f 3 2 27
	                String[] faceIndices = line.split(" ");
	                indices.add(Integer.valueOf(faceIndices[1])-1);
	                indices.add(Integer.valueOf(faceIndices[2])-1);
	                indices.add(Integer.valueOf(faceIndices[3])-1);
	            } else {
	                System.err.println("[OBJ] Unknown Line: " + line);
	            }
	        }
	        float[] vertexArr = FileHandler.toFloats(vertices);
//	        float[] uvArr = FileHandler.toFloats(uvs);
	        int[] indicesArr = FileHandler.toInts(indices);
	        reader.close();
	        RawModel model = loadVAO(name, drawMethod, vertexArr, indicesArr);
	        model.setLifeTicks(-500);
	        if (model!=null){
	        	System.out.println("[OBJ] Model loaded.");
	        }
	        return model;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
    }

	public int loadTexture(String path){
		Texture texture;
		if (textures.containsKey(path)){
			return textures.get(path);
		}else{
			try {
				if (path.endsWith(".dds")){
					File tga = ImageConverter.convert(new File(path), ImageType.TGA, true);
					texture = TextureLoader.getTexture("TGA", new FileInputStream(tga.getAbsolutePath()));
				}else{
					texture = TextureLoader.getTexture("PNG", new FileInputStream(path));
				}
				textures.put(path, texture.getTextureID());
				return texture.getTextureID();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Unable to load Texture - FILE NOT FOUND! "+path);
				return notFoundID;
			}	
		}
	}
	
	
	
	private void storeDataAsAttr(int index, int dimensions, float[] data){
		int vboID = GL15.glGenBuffers();
		vbos.add(vboID);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
		FloatBuffer buffer = getFloatBuffer(data);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(index, dimensions, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
		
	}
	
	private void storeDataInAttributeList(int attributeNumber, int vectorSize, float[] data){
		int vboID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
		FloatBuffer buffer = storeDataInFloatBuffer(data);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(attributeNumber, vectorSize, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}
	
	public void unbindVAO(){
		GL30.glBindVertexArray(0);
	}
	
	public void bindIndiciesBuffer(int[] indices){
		int vboID = GL15.glGenBuffers();
		vbos.add(vboID);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID);
		IntBuffer buffer = getIntBuffer(indices);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
	}
	
	
	public IntBuffer getIntBuffer(int[] data){
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
	
	public FloatBuffer getFloatBuffer(float[] data){
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
	
	private FloatBuffer storeDataInFloatBuffer(float[] data){
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	public ArrayList<Integer> getVaos() {
		return vaos;
	}

	public ArrayList<Integer> getVbos() {
		return vbos;
	}

	public HashMap<String, Integer> getTextures() {
		return textures;
	}
	
}
