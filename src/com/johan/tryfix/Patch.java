package com.johan.tryfix;

import java.io.File;
import java.io.IOException;

public class Patch {
	
	public static void main(String[] args) {
		
		// 检查
		if (args.length != 2) {
			System.out.println("参数1 : 原始apk路径");
			System.out.println("参数2 : 修复apk路径");
			return;
		}
		
		System.out.println("原始APK路径：" + args[0]);
		System.out.println("修复APK路径：" + args[1]);
		
		// 获取路径
		String sourceApkPath = new File(args[0]).getAbsolutePath();
		String fixApkPath = new File(args[1]).getAbsolutePath();
		
		System.out.println("===================================== 检测 ==================================");
		
		if (!sourceApkPath.contains(".")) {
			System.out.println("原始APK路径应该是xxx.apk");
			return;
		}
		
		if (!fixApkPath.contains(".")) {
			System.out.println("修复APK路径应该是xxx.apk");
			return;
		}
		
		if (!FileManager.isExist(sourceApkPath)) {
			System.out.println("没找到原始APK");
			return;
		}
		
		if (!FileManager.isExist(fixApkPath)) {
			System.out.println("没找到修复APK");
			return;
		}
		
		System.out.println("===================================== 开始 ==================================");
		
		boolean result;
		
		// 解压路径
		int index = sourceApkPath.lastIndexOf(".");
		String sourceUnzipPath = sourceApkPath.substring(0, index);
		index = fixApkPath.lastIndexOf(".");
		String fixUnzipPath = fixApkPath.substring(0, index);
		String patchUnzipPath = new File(sourceApkPath).getParent() + File.separator + "patch";
		
		
		try {
			
			// 解压
			result = unzipApk(sourceApkPath, sourceUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			result = unzipApk(fixApkPath, fixUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 工具文件路径
			String dex2jarPath = new File(sourceApkPath).getParent() + File.separator + "dex2jar-2.0";
			
			// 从原始APK提取Class文件
			result = takeClasses(sourceUnzipPath, dex2jarPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			// 从修复APK提取Class文件
			result = takeClasses(fixUnzipPath, dex2jarPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 对比原始和修复的Class文件，取出有差异的class文件
			result = takeDifferenceClasses(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 打包 Class -> Jar -> Dex
			result = packageClasses(patchUnzipPath, dex2jarPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 对比原始和修复的res文件，取出有差异的res文件
			result = takeDifferenceResource(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 对比原始和修复的lib文件，取出有差异的lib文件
			result = takeDifferenceLib(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 清空空文件夹
			result = clearEmpty(patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 拷贝resources.arsc
			result = copyArsc(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// 压缩
			result = compress(patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
		
		} finally {
			
			// 清理
			clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			
		}
		
	}

	/**
	 * 解压APK
	 * @param apkPath
	 * @param unzipPath
	 * @return
	 */
	private static boolean unzipApk(String apkPath, String unzipPath) {
		try {
			FileManager.unzip(apkPath, unzipPath);
			System.out.println(apkPath + "解压到" + unzipPath);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 从APK提取Class文件
	 * @param unzipPath
	 * @param dex2jarPath
	 */
	private static boolean takeClasses(String unzipPath, String dex2jarPath) {
		
		System.out.println("apkUnzipPath : " + unzipPath);
		System.out.println("dex2jarPath : " + dex2jarPath);
		
		// 复制 classes.dex 到 dex2jar工具包中
		String dexSourcePath = unzipPath + File.separator + "classes.dex";
		String dexTargetPath = dex2jarPath + File.separator + "classes.dex";
		try {
			FileManager.copyFile(dexSourcePath, dexTargetPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("复制 : " + dexSourcePath + " -> " + dexTargetPath);
		
		// cmd dex2jar classes.dex文件 解压 classes-dex2jar.jar文件
		try {
			CMD.execute(dex2jarPath + File.separator + "dex2jar.bat");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("dex->jar : " + dexTargetPath);
		
		// 解压jar包 classes-dex2jar.jar
		String jarPath = dex2jarPath + File.separator + "classes-dex2jar.jar";
		String classesPath = unzipPath + File.separator + "classes";
		if (!FileManager.isExist(jarPath)) {
			System.out.println("dex->jar 失败 ");
			return false;
		}
		try {
			FileManager.unzip(jarPath, classesPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("解压 : " + jarPath + " -> " + classesPath);
		
		// 删除在dex2jar工具包中的classes.dex和classes-dex2jar.jar文件
		FileManager.delete(dexTargetPath);
		System.out.println("删除 : " + dexTargetPath);
		FileManager.delete(jarPath);
		System.out.println("删除 : " + jarPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * 取出差异的class文件，放在apk所在目录的patch文件夹
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static boolean takeDifferenceClasses(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String sourceClassesPath = sourceUnzipPath + File.separator + "classes";
		String fixClassesPath = fixUnzipPath + File.separator + "classes";
		
		String patchClassesPath = patchUnzipPath + File.separator + "classes";
		FileManager.createDir(patchClassesPath);
		
		// 将fixApk的classes文件复制到patch文件夹
		try {
			FileManager.copyDir(fixClassesPath, patchClassesPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("复制 : " + fixClassesPath + " -> " + patchClassesPath);
		
		// 去掉和sourceApk相同的Classes
		FileManager.compare(sourceClassesPath, patchClassesPath);
		System.out.println("对比 : " + sourceClassesPath + " <-> " + patchClassesPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * 打包 classes -> jar -> dex
	 * @param patchUnzipPath
	 * @param dex2jarPath
	 */
	private static boolean packageClasses(String patchUnzipPath, String dex2jarPath) {
		
		String patchClassesPath = patchUnzipPath + File.separator + "classes";
		
		// 将有修改的Class打jar包
		String classes2jarBatPath = dex2jarPath + File.separator + "classes2jar.bat";
		String patchJarPath = dex2jarPath + File.separator + "classes.jar";
		// cmd jar classes文件 打包 classes.jar文件
		try {
			CMD.execute(classes2jarBatPath, patchClassesPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("打Jar包 : " + patchClassesPath + "=>" + patchJarPath);
		
		// cmd jar2dex classes.jar文件 打包 classes.dex文件
		String patchDexPath = dex2jarPath + File.separator + "classes.dex";
		try {
			CMD.execute(dex2jarPath + File.separator + "jar2dex.bat");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("jar->dex : " + patchJarPath + "=>" + patchDexPath);
		
		// 将 patch.dex 复制到 与apk相同目录
		String patchDexTargetPath = patchUnzipPath + File.separator + "classes.dex";
		try {
			FileManager.copyFile(patchDexPath, patchDexTargetPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("复制 : " + patchDexPath + " -> " + patchDexTargetPath);
		
		// 删除classes文件夹
		FileManager.clear(patchClassesPath);
		System.out.println("删除 : " + patchClassesPath);
		
		// 删除在dex2jar工具包中的classes.jar和classes.dex文件
		FileManager.delete(patchJarPath);
		System.out.println("删除 : " + patchJarPath);
		FileManager.delete(patchDexPath);
		System.out.println("删除 : " + patchDexPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * 取出差异的res文件，放在patch文件夹
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static boolean takeDifferenceResource(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String sourceResPath = sourceUnzipPath + File.separator + "res";
		String fixResPath = fixUnzipPath + File.separator + "res";
		
		String patchResPath = patchUnzipPath + File.separator + "res";
		FileManager.createDir(patchResPath);
		
		// 将fixApk的res文件夹复制到patch文件夹
		try {
			FileManager.copyDir(fixResPath, patchResPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("复制 : " + fixResPath + " -> " + patchResPath);
		
		// 去掉和sourceApk相同的Res
		FileManager.compare(sourceResPath, patchResPath);
		System.out.println("对比 : " + sourceResPath + " <-> " + patchResPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * 取出差异的lib文件，放在patch文件夹
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static boolean takeDifferenceLib(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String sourceLibPath = sourceUnzipPath + File.separator + "lib";
		String fixLibPath = fixUnzipPath + File.separator + "lib";
		
		String patchLibPath = patchUnzipPath + File.separator + "lib";
		FileManager.createDir(patchLibPath);
		
		// 将fixApk的lib文件夹复制到patch文件夹
		try {
			FileManager.copyDir(fixLibPath, patchLibPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("复制 : " + fixLibPath + " -> " + patchLibPath);
		
		// 去掉和sourceApk相同的Lib
		FileManager.compare(sourceLibPath, patchLibPath);
		System.out.println("对比 : " + sourceLibPath + " <-> " + patchLibPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * 删除空的文件夹
	 * @return
	 */
	private static boolean clearEmpty(String patchUnzipPath) {
		
		try {
			String resPath = patchUnzipPath + File.separator + "res";
			FileManager.deleteEmptyDirectory(resPath);
			System.out.println("清空  " + resPath + " 空文件夹");
			
			String libPath = patchUnzipPath + File.separator + "lib";
			FileManager.deleteEmptyDirectory(libPath);
			System.out.println("清空  " + libPath + " 空文件夹");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		System.out.println("=======================================================================");
		
		return true;
	}
	
	/**
	 * 清除空文件之后，
	 * 1.如果原始和修复的resources.arsc不相同，拷贝 resources.arsc
	 * 2.如果还有res文件夹，拷贝 resources.arsc
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 * @return
	 */
	private static boolean copyArsc(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String patchResPath = patchUnzipPath + File.separator + "res";
		File patchResFile = new File(patchResPath);
		
		String sourceArscPath = sourceUnzipPath + File.separator + "resources.arsc";
		String fixArscPath = fixUnzipPath + File.separator + "resources.arsc";
		String patchArscPath = patchUnzipPath + File.separator + "resources.arsc";
		
		boolean compareResult = FileManager.isSame(sourceArscPath, fixArscPath);
		
		System.out.println("对比 : " + sourceArscPath + " <-> " + fixArscPath + " = " + compareResult);
		
		if (!compareResult || patchResFile.exists()) {
			try {
				FileManager.copyFile(fixArscPath, patchArscPath);
				System.out.println("复制 : " + fixArscPath + " -> " + patchArscPath);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		System.out.println("=======================================================================");
		
		return true;
	}
	
	/**
	 * 压缩 
	 * @param patchUnzipPath
	 */
	private static boolean compress(String patchUnzipPath) {
		
		File patchApk = new File(patchUnzipPath);
		String patchApkPath = patchApk.getParent() + File.separator + "patch.apk";
	
		// 压缩 patch文件夹 -> patch.apk
		FileManager.zip(patchUnzipPath, patchApkPath);
		System.out.println("压缩 : " + patchUnzipPath + " -> " + patchApkPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * 清理中途生成的文件
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static void clear(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		// 清空原始APK生成的文件
		FileManager.clear(sourceUnzipPath);
		System.out.println("清理 : " + sourceUnzipPath);
		
		// 清空修复APK生成的文件
		FileManager.clear(fixUnzipPath);
		System.out.println("清理 : " + fixUnzipPath);
		
		// 清空Patch文件
		FileManager.clear(patchUnzipPath);
		System.out.println("清理 : " + patchUnzipPath);
		
		System.out.println("===================================== 结束 ==================================");
		
	}
	
}
