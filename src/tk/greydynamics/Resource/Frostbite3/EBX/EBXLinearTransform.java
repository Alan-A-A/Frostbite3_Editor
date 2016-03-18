package tk.greydynamics.Resource.Frostbite3.EBX;

import java.util.ArrayList;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import tk.greydynamics.JavaFX.Windows.EBXWindow;
import tk.greydynamics.Maths.Matrices;
import tk.greydynamics.Resource.FileHandler;
import tk.greydynamics.Resource.Frostbite3.EBX.EBXHandler.FieldValueType;

public class EBXLinearTransform {
	
	private ArrayList<Vector3f> tranformations;
		
	//Consturctor
	public EBXLinearTransform(ArrayList<Vector3f> tranformations){
		this.tranformations = tranformations;
	}
	
	public EBXLinearTransform(EBXComplex ebxComplex){
		this.tranformations = new ArrayList<>();
		if (ebxComplex.getComplexDescriptor().getName().equals("LinearTransform")){
			for (EBXField part : ebxComplex.getFields()){
				Vector3f vec3 = new Vector3f();
				EBXComplex partComplex = part.getValueAsComplex();
				for (EBXField v : partComplex.getFields()){
					switch (v.getFieldDescritor().getName()){
						case "x":
							if (v.getValue()==null){
								vec3.setX(-1f);
								break;
							}
							vec3.setX((float) v.getValue());
							break;
						case "y":
							if (v.getValue()==null){
								vec3.setY(-1f);
								break;
							}
							vec3.setY((float) v.getValue());
							break;
						case "z":
							if (v.getValue()==null){
								vec3.setZ(-1f);
								break;
							}
							vec3.setZ((float) v.getValue());
							break;
					}
				}
				this.tranformations.add(vec3);
			}
			if (this.tranformations.get(0).getX()==-1.0f&&this.tranformations.get(0).getY()==-1.0f&&this.tranformations.get(0).getZ()==-1.0f){
				//LinearTransform without data. Fix this.
				if (this.tranformations.size()==4){
					this.tranformations.set(0, new Vector3f(1.0f, 0.0f, 0.0f));
					this.tranformations.set(1, new Vector3f(0.0f, 1.0f, 0.0f));
					this.tranformations.set(2, new Vector3f(0.0f, 0.0f, 1.0f));
					this.tranformations.set(3, new Vector3f(0.0f, 0.0f, 0.0f));
				}else{
					System.err.println("Some other kind of LinearTransform found.");
				}
			}
		}
	}
	
	public Vector3f getRotation(){
		return Matrices.getRotationInEulerAngles(this.tranformations.get(0), this.tranformations.get(1), this.tranformations.get(2));
	}
	
	public Vector3f getTranformation(){
		return this.tranformations.get(3);
	}

	public Vector3f getScaling() {
		Matrix4f matrix = new Matrix4f();
		//Create Transformation Matrix from Data.
		matrix.m00 = this.tranformations.get(0).getX();
		matrix.m01 = this.tranformations.get(0).getY();
		matrix.m02 = this.tranformations.get(0).getZ();
		
		matrix.m10 = this.tranformations.get(1).getX();
		matrix.m11 = this.tranformations.get(1).getY();
		matrix.m12 = this.tranformations.get(1).getZ();
		
		matrix.m20 = this.tranformations.get(2).getX();
		matrix.m21 = this.tranformations.get(2).getY();
		matrix.m22 = this.tranformations.get(2).getZ();
		
		matrix.m03 = this.tranformations.get(3).getX();
		matrix.m13 = this.tranformations.get(3).getY();
		matrix.m23 = this.tranformations.get(3).getZ();
		
				
		Matrix4f invertedRotationMatrix = (Matrix4f) Matrices.createTransformationMatrix(getTranformation(), getRotation(), new Vector3f(1.0f, 1.0f, 1.0f)).invert();

		Matrix4f scaleMatrix = Matrix4f.mul(matrix, invertedRotationMatrix, null);
//		System.out.println(scaleMatrix);
				
		return new Vector3f(scaleMatrix.m00, scaleMatrix.m11, scaleMatrix.m22);
	}
	
	public static void setTransformation(Matrix4f matrix, EBXComplex linearTransformComplex, EBXWindow window, boolean writeByteArr){
		try {
			if (linearTransformComplex.getComplexDescriptor().getName().equals("LinearTransform")){
				/* m00, m10, m20, m30 *
				 * m01, m11, m21, m31 *
				 * m02, m12, m22, m32 *
				 * m03, m13, m23, m33 *
				 * 
				 * 		|
				 * 		|
				 * 		|
				 * 		V
				 * 
				 * rX, rY, rZ, tX *
				 * uX, uY, uZ, tY *
				 * fX, fY, fZ, tZ *
				 * 0 , 0 , 0 , 1  */
				
				EBXComplex right = linearTransformComplex.getField(0).getValueAsComplex();
					right.getField(0).setValue(matrix.m00, FieldValueType.Float);//x
					right.getField(1).setValue(matrix.m10, FieldValueType.Float);//y
					right.getField(2).setValue(matrix.m20, FieldValueType.Float);//z
					
				EBXComplex up = linearTransformComplex.getField(1).getValueAsComplex();
					up.getField(0).setValue(matrix.m01, FieldValueType.Float);//x
					up.getField(1).setValue(matrix.m11, FieldValueType.Float);//y
					up.getField(2).setValue(matrix.m21, FieldValueType.Float);//z
				
				EBXComplex forward = linearTransformComplex.getField(2).getValueAsComplex();
					forward.getField(0).setValue(matrix.m02, FieldValueType.Float);//x
					forward.getField(1).setValue(matrix.m12, FieldValueType.Float);//y
					forward.getField(2).setValue(matrix.m22, FieldValueType.Float);//z
				
				EBXComplex transform = linearTransformComplex.getField(3).getValueAsComplex();
					transform.getField(0).setValue(matrix.m30, FieldValueType.Float);//x
					transform.getField(1).setValue(matrix.m31, FieldValueType.Float);//y
					transform.getField(2).setValue(matrix.m32, FieldValueType.Float);//z
					
				if (window!=null){
					window.refresh();
					if (writeByteArr){
						byte[] originalBytes = window.getController().getOriginalBytes();
						if (originalBytes!=null){
							FileHandler.addBytes(FileHandler.toBytes((float) right.getField(0).getValue()), originalBytes, right.getField(0).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) right.getField(1).getValue()), originalBytes, right.getField(1).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) right.getField(2).getValue()), originalBytes, right.getField(2).getOffset());
							
							FileHandler.addBytes(FileHandler.toBytes((float) up.getField(0).getValue()), originalBytes, up.getField(0).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) up.getField(1).getValue()), originalBytes, up.getField(1).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) up.getField(2).getValue()), originalBytes, up.getField(2).getOffset());
							
							FileHandler.addBytes(FileHandler.toBytes((float) forward.getField(0).getValue()), originalBytes, forward.getField(0).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) forward.getField(1).getValue()), originalBytes, forward.getField(1).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) forward.getField(2).getValue()), originalBytes, forward.getField(2).getOffset());
							
							FileHandler.addBytes(FileHandler.toBytes((float) transform.getField(0).getValue()), originalBytes, transform.getField(0).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) transform.getField(1).getValue()), originalBytes, transform.getField(1).getOffset());
							FileHandler.addBytes(FileHandler.toBytes((float) transform.getField(2).getValue()), originalBytes, transform.getField(2).getOffset());
							
						}else{
							System.err.println("Can't write Matrix to EBXByteArr: OriginalBytes from EBXWindow are missing!");
						}
					}
				}
				
			}
		}catch (Exception e){
			e.printStackTrace();
			System.err.println("Can't pass Matrix values to LinearTransformComplex!");
		}
	}
}
