package tk.greydynamics.Game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import tk.greydynamics.Messages;
import tk.greydynamics.Entity.EntityHandler;
import tk.greydynamics.JavaFX.JavaFXHandler;
import tk.greydynamics.JavaFX.TreeViewConverter;
import tk.greydynamics.JavaFX.TreeViewEntry;
import tk.greydynamics.JavaFX.Windows.MainWindow.EntryType;
import tk.greydynamics.Mod.Mod;
import tk.greydynamics.Model.ModelHandler;
import tk.greydynamics.Player.PlayerEntity;
import tk.greydynamics.Player.PlayerHandler;
import tk.greydynamics.Render.Gui.GuiTexture;
import tk.greydynamics.Resource.FileHandler;
import tk.greydynamics.Resource.ResourceHandler;
import tk.greydynamics.Resource.Frostbite3.Cas.Bundle;
import tk.greydynamics.Resource.Frostbite3.Cas.Data.ResourceFinder;
import tk.greydynamics.Resource.Frostbite3.ITEXTURE.ITextureHandler;
import tk.greydynamics.Resource.Frostbite3.Layout.LayoutFile;
import tk.greydynamics.Resource.Frostbite3.Toc.ConvertedTocFile;
import tk.greydynamics.Resource.Frostbite3.Toc.TocConverter;
import tk.greydynamics.Resource.Frostbite3.Toc.TocManager;
import tk.greydynamics.Shader.ShaderHandler;
import tk.greydynamics.Terrain.TerrainHandler;

public class Game {
	private ModelHandler modelHandler;
	private TerrainHandler terrainHandler;
	private PlayerHandler playerHandler;
	private ResourceHandler resourceHandler;
	private EntityHandler entityHandler;
	private ShaderHandler shaderHandler;
	private ITextureHandler itextureHandler;
	private String currentFile;
	private ConvertedTocFile currentToc;
	private Bundle currentBundle;
	private HashMap<String, String> chunkGUIDSHA1;
	private Mod currentMod;
	private ArrayList<GuiTexture> guis;
	
	private ArrayList<ConvertedTocFile> commonChunks;
					
	public Game(){
		currentMod = null;
		/*LEVEL OF DETAIL
		 * 0=100%
		 * 1=50%
		 * 2=25%
		 * 3=12.5%
		 * ....
		 * MAX 9!
		 */
		
		modelHandler = new ModelHandler();
		
		playerHandler = new PlayerHandler();
		
		/*DO NOT CARE ABOUT IN THE MOMENT*/
		terrainHandler = new TerrainHandler();
		terrainHandler.generate(0, 0);//defined from terrain4k.decals -> terrain subpackage
		//terrainHandler.generate(0, 1);
				
		resourceHandler = new ResourceHandler();
		
		shaderHandler = new ShaderHandler();
		entityHandler = new EntityHandler(modelHandler, resourceHandler);
		
		guis = new ArrayList<>();
		
		if (!Core.isDEBUG){
			System.out.println(Messages.getString("Game.0")); //$NON-NLS-1$
			Core.getJavaFXHandler().getMainWindow().selectGamePath();
		}else{
			
			/*TEST FOR PATCHING BASEDATA USING DELTA
			byte[] patchedData = Patcher.getPatchedData(
					FileHandler.readFile("__DOCUMENTATION__/patch_system/decompressed_base"),
					FileHandler.readFile("__DOCUMENTATION__/patch_system/delta")
			);
			FileHandler.writeFile("output/patched_data", patchedData);
			END OF TEST*/
		}
		
		while (Core.keepAlive){
			//wait
			System.out.print(""); //$NON-NLS-1$
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (Core.gamePath != null && Core.getJavaFXHandler().getMainWindow().getModLoaderWindow() != null){
				break;
			}
		}
		resourceHandler.createEBXComponentHandler(Core.gameName);
		checkGameVersion();
		Core.getJavaFXHandler().getMainWindow().getModLoaderWindow().getController().setGamepath(FileHandler.normalizePath(Core.gamePath));
		Core.getJavaFXHandler().getMainWindow().toggleModLoaderVisibility();
		File cascat = new File(Core.gamePath+Core.PATH_DATA+"cas.cat"); //$NON-NLS-1$
//		if (!cascat.exists()){
//			//System.err.println("Invalid gamepath selected.");
//			Core.getJavaFXHandler().getDialogBuilder().showError("ERROR", "Invalid gamepath selected.", null);
//			Core.keepAlive(false);
//		}else{
			System.out.println(Messages.getString("Game.1")); //$NON-NLS-1$
			buildEditor();
			
			if (Core.isDEBUG){//EBX-DEBUG
				Core.getJavaFXHandler().getMainWindow().toggleModLoaderVisibility();
				currentMod = null;
				//ebxFileGUIDs = new HashMap<>();
				//ebxFileGUIDs.put("EA830D5EFFB3EE489D44963370D466B1", "test/test1/test2");
				/*byte[] bytes = FileHandler.readFile("__DOCUMENTATION__/ebx/sample_ebx/layer0_default.ebx");
				byte[] bytes = FileHandler.readFile("mods/SampleMod/resources/levels/mp/mp_playground/content/layer2_buildings.bak--IGNORE");
				EBXFile ebxFile = resourceHandler.getEBXHandler().loadFile(bytes);
				TreeItem<TreeViewEntry> treeView = TreeViewConverter.getTreeView(ebxFile);
				Core.getJavaFXHandler().setTreeViewStructureRight(treeView);
				Core.getJavaFXHandler().getMainWindow().updateRightRoot();*/
//			}
		}
	}
	
	
	private boolean checkGameVersion() {
		if (Core.gamePath!=null){
			//{"sku":"origin","build":"923305"}
			//TODO just grep game version.
			try{
				FileReader fr = new FileReader(Core.gamePath+"/version.json"); //$NON-NLS-1$
				BufferedReader br = new BufferedReader(fr);
			    String line = br.readLine();
			    Core.gameVersion = line;
			    Core.getJavaFXHandler().getMainWindow().getModLoaderWindow().getController().getGameVersionLabel().setText(Messages.getString("Game.4")+Core.gameVersion); //$NON-NLS-1$
			    br.close();
			    fr.close();
			}catch (FileNotFoundException e){
				Core.getJavaFXHandler().getDialogBuilder().showError(Messages.getString("Game.3"), Messages.getString("Game.2"), null); //$NON-NLS-1$ //$NON-NLS-2$
				Core.gameVersion = "not found"; //$NON-NLS-1$
			}catch (Exception e){
				e.printStackTrace();
				Core.getJavaFXHandler().getDialogBuilder().showError("ERROR", Messages.getString("Game.5"), null); //$NON-NLS-1$ //$NON-NLS-2$
				Core.gameVersion = "not compatible"; //$NON-NLS-1$
			}
		}
		return false;
	}


	public void buildEditor(){
		File normalCasCat = new File(Core.gamePath+Core.PATH_DATA+"cas.cat"); //$NON-NLS-1$
		if (normalCasCat.exists()){
			resourceHandler.getCasCatManager().readCat(FileHandler.readFile(Core.gamePath+Core.PATH_DATA+"cas.cat"), "normal"); //$NON-NLS-1$ //$NON-NLS-2$
			File patchedCasCat = new File(Core.gamePath+Core.PATH_UPDATE_PATCH+Core.PATH_DATA+"cas.cat"); //$NON-NLS-1$
			if (patchedCasCat.exists()){
				resourceHandler.getPatchedCasCatManager().readCat(FileHandler.readFile(patchedCasCat.getAbsolutePath()), "patched"); //$NON-NLS-1$
			}
		}else{
			System.err.println(Messages.getString("Game.6")); //$NON-NLS-1$
		}
		//ebxFileGUIDs = new HashMap<String, String>();
		chunkGUIDSHA1 = new HashMap<String, String>();
		
		/*Use this to fetch common chunks!*/
		commonChunks = new ArrayList<ConvertedTocFile>();
		for (File file : FileHandler.listf(Core.gamePath+"/", "Chunks")){ //$NON-NLS-1$ //$NON-NLS-2$
			if (file.getAbsolutePath().endsWith(".toc")){ //$NON-NLS-1$
				try{
					String relPath = file.getAbsolutePath().replace("\\", "/").replace(".toc", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					LayoutFile toc = TocManager.readToc(relPath);
					ConvertedTocFile convToc = TocConverter.convertTocFile(toc);
					commonChunks.add(convToc);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		/*End of common chunks!*/
		
		/*Battlefield Weapons and Attachments*/
		for (File file : FileHandler.listf(Core.gamePath+"/", "WeaponsAndAttachments")){ //$NON-NLS-1$ //$NON-NLS-2$
			if (file.getAbsolutePath().endsWith(".toc")){ //$NON-NLS-1$
				try{
					String relPath = file.getAbsolutePath().replace("\\", "/").replace(".toc", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					LayoutFile toc = TocManager.readToc(relPath);
					ConvertedTocFile convToc = TocConverter.convertTocFile(toc);
					commonChunks.add(convToc);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
	}

	
	public void update(){
		playerHandler.update();	
		terrainHandler.collisionUpdate(playerHandler);
	}
	
	public void lowRateUpdate(){
		PlayerEntity pe = playerHandler.getPlayerEntity();
		//entityHandler.getFocussedEntity(pe.getPos(), pe.getRot());
	}
	
	public void buildExplorerTree(){		
		currentToc = null;
		currentBundle = null;
		ArrayList<File> patch = FileHandler.listf(Core.gamePath+Core.PATH_UPDATE_PATCH, ".sb"); //$NON-NLS-1$
		ArrayList<File> data = FileHandler.listf(Core.gamePath+Core.PATH_DATA, ".sb"); //$NON-NLS-1$
		ArrayList<File> xp = FileHandler.listf(null, Core.gamePath+Core.PATH_UPDATE, ".sb", null, Core.PATH_UPDATE_PATCH);//Just find DLC base //$NON-NLS-1$
		
		TreeItem<TreeViewEntry> explorerTree = new TreeItem<TreeViewEntry>(new TreeViewEntry(Core.gamePath, null, null, EntryType.LIST));
		
		for (File file : patch){
			buildExplorerTreeFile(file, explorerTree, Color.LIGHTBLUE, true);
		}
		
		for (File file : data){
			File patched = ResourceFinder.findPatch(file.getAbsolutePath());
			if (!patched.exists()){
				buildExplorerTreeFile(file, explorerTree, Color.LIGHTGREY, true);
			}
		}
		
		for (File file : xp){
			File patched = ResourceFinder.findXPackPatch(file.getAbsolutePath());
			if (patched!=null){
				if (!patched.exists()){
					buildExplorerTreeFile(file, explorerTree, Color.LIGHTGREEN, true);
				}
			}
			
		}
		
		explorerTree.getValue();
		for (TreeItem<TreeViewEntry> child : explorerTree.getChildren()){
			if (child.getChildren().size()>0){
				child.setExpanded(true);
			}
		}
		//Core.getJavaFXHandler().setTreeViewStructureLeft(explorerTree);
		Core.getJavaFXHandler().getMainWindow().setPackageExplorer(explorerTree);
		
		//Core.getJavaFXHandler().setTreeViewStructureLeft1(null);
		Core.getJavaFXHandler().getMainWindow().setPackageExplorer1(null, null);
		
		/*
		Core.getJavaFXHandler().setTreeViewStructureRight(null);
		Core.getJavaFXHandler().getMainWindow().updateRightRoot();
		
		*/
	}
	private boolean buildExplorerTreeFile(File file, TreeItem<TreeViewEntry> explorerTree, Color backgroundColor, boolean bundleTypeIcon){
		String relPath = file.getAbsolutePath().replace("\\", "/").replace(".sb", "").replace(Core.gamePath+"/", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		ImageView icon = null;
		if (bundleTypeIcon){
			try{
				LayoutFile toc = TocManager.readToc(Core.gamePath+"/"+relPath); //$NON-NLS-1$
				ConvertedTocFile convToc = TocConverter.convertTocFile(toc);
				if (convToc.isCas()){
					icon = new ImageView(JavaFXHandler.ICON_3_ORANGE);
				}else{
					icon = new ImageView(JavaFXHandler.ICON_2_BLUE);
				}
			}catch(Exception e){return false;}
		}
		if (icon==null){
			icon = new ImageView(JavaFXHandler.ICON_DOCUMENT);
		}
		
		String[] fileName = relPath.split("/"); //$NON-NLS-1$
		
		TreeItem<TreeViewEntry> convTocTree = new TreeItem<TreeViewEntry>(new TreeViewEntry(fileName[fileName.length-1], icon, file, EntryType.LIST)); 
		TreeViewConverter.pathToTree(explorerTree, relPath, convTocTree, backgroundColor);
		return true;
	}
	
	
	//<----------GETTER AND SETTER--------------->//
	
	/*Handler*/
	public ResourceHandler getResourceHandler() {
		return resourceHandler;
	}
		
	public PlayerHandler getPlayerHandler(){
		return playerHandler;
	}

	public ModelHandler getModelHandler() {
		return modelHandler;
	}

	public TerrainHandler getTerrainHandler() {
		return terrainHandler;
	}

	public EntityHandler getEntityHandler() {
		return entityHandler;
	}

	public ShaderHandler getShaderHandler() {
		return shaderHandler;
	}

	public ITextureHandler getITextureHandler() {
		return itextureHandler;
	}
	/*End of Handler*/
	
	
	/*Game*/

	public String getCurrentFile() {
		return currentFile;
	}


	public void setCurrentFile(String currentFile) {
		this.currentFile = currentFile;
	}


	/*public HashMap<String, String> getEBXFileGUIDs() {
		return ebxFileGUIDs;
	}*/


	public ConvertedTocFile getCurrentToc() {
		return currentToc;
	}


	public void setCurrentToc(ConvertedTocFile currentToc) {
		this.currentToc = currentToc;
	}


	public Bundle getCurrentBundle() {
		return currentBundle;
	}


	public void setCurrentBundle(Bundle currentBundle) {
		this.currentBundle = currentBundle;
	}


	public HashMap<String, String> getChunkGUIDSHA1() {
		return chunkGUIDSHA1;
	}


	public Mod getCurrentMod() {
		return currentMod;
	}


	public void setCurrentMod(Mod currentMod) {
		this.currentMod = currentMod;
	}


	public ArrayList<ConvertedTocFile> getCommonChunks() {
		return commonChunks;
	}


	public ArrayList<GuiTexture> getGuis() {
		return guis;
	}

	
	
	
	/*End of Game*/
	
	
	
	
	
}
