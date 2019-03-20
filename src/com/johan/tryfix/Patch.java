package com.johan.tryfix;

import java.io.File;
import java.io.IOException;

public class Patch {
	
	public static void main(String[] args) {
		
		// ���
		if (args.length != 2) {
			System.out.println("����1 : ԭʼapk·��");
			System.out.println("����2 : �޸�apk·��");
			return;
		}
		
		System.out.println("ԭʼAPK·����" + args[0]);
		System.out.println("�޸�APK·����" + args[1]);
		
		// ��ȡ·��
		String sourceApkPath = new File(args[0]).getAbsolutePath();
		String fixApkPath = new File(args[1]).getAbsolutePath();
		
		System.out.println("===================================== ��� ==================================");
		
		if (!sourceApkPath.contains(".")) {
			System.out.println("ԭʼAPK·��Ӧ����xxx.apk");
			return;
		}
		
		if (!fixApkPath.contains(".")) {
			System.out.println("�޸�APK·��Ӧ����xxx.apk");
			return;
		}
		
		if (!FileManager.isExist(sourceApkPath)) {
			System.out.println("û�ҵ�ԭʼAPK");
			return;
		}
		
		if (!FileManager.isExist(fixApkPath)) {
			System.out.println("û�ҵ��޸�APK");
			return;
		}
		
		System.out.println("===================================== ��ʼ ==================================");
		
		boolean result;
		
		// ��ѹ·��
		int index = sourceApkPath.lastIndexOf(".");
		String sourceUnzipPath = sourceApkPath.substring(0, index);
		index = fixApkPath.lastIndexOf(".");
		String fixUnzipPath = fixApkPath.substring(0, index);
		String patchUnzipPath = new File(sourceApkPath).getParent() + File.separator + "patch";
		
		
		try {
			
			// ��ѹ
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
			
			// �����ļ�·��
			String dex2jarPath = new File(sourceApkPath).getParent() + File.separator + "dex2jar-2.0";
			
			// ��ԭʼAPK��ȡClass�ļ�
			result = takeClasses(sourceUnzipPath, dex2jarPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			// ���޸�APK��ȡClass�ļ�
			result = takeClasses(fixUnzipPath, dex2jarPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// �Ա�ԭʼ���޸���Class�ļ���ȡ���в����class�ļ�
			result = takeDifferenceClasses(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// ��� Class -> Jar -> Dex
			result = packageClasses(patchUnzipPath, dex2jarPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// �Ա�ԭʼ���޸���res�ļ���ȡ���в����res�ļ�
			result = takeDifferenceResource(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// �Ա�ԭʼ���޸���lib�ļ���ȡ���в����lib�ļ�
			result = takeDifferenceLib(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// ��տ��ļ���
			result = clearEmpty(patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// ����resources.arsc
			result = copyArsc(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
			
			// ѹ��
			result = compress(patchUnzipPath);
			if (!result) {
				clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
				return;
			}
		
		} finally {
			
			// ����
			clear(sourceUnzipPath, fixUnzipPath, patchUnzipPath);
			
		}
		
	}

	/**
	 * ��ѹAPK
	 * @param apkPath
	 * @param unzipPath
	 * @return
	 */
	private static boolean unzipApk(String apkPath, String unzipPath) {
		try {
			FileManager.unzip(apkPath, unzipPath);
			System.out.println(apkPath + "��ѹ��" + unzipPath);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * ��APK��ȡClass�ļ�
	 * @param unzipPath
	 * @param dex2jarPath
	 */
	private static boolean takeClasses(String unzipPath, String dex2jarPath) {
		
		System.out.println("apkUnzipPath : " + unzipPath);
		System.out.println("dex2jarPath : " + dex2jarPath);
		
		// ���� classes.dex �� dex2jar���߰���
		String dexSourcePath = unzipPath + File.separator + "classes.dex";
		String dexTargetPath = dex2jarPath + File.separator + "classes.dex";
		try {
			FileManager.copyFile(dexSourcePath, dexTargetPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("���� : " + dexSourcePath + " -> " + dexTargetPath);
		
		// cmd dex2jar classes.dex�ļ� ��ѹ classes-dex2jar.jar�ļ�
		try {
			CMD.execute(dex2jarPath + File.separator + "dex2jar.bat");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("dex->jar : " + dexTargetPath);
		
		// ��ѹjar�� classes-dex2jar.jar
		String jarPath = dex2jarPath + File.separator + "classes-dex2jar.jar";
		String classesPath = unzipPath + File.separator + "classes";
		if (!FileManager.isExist(jarPath)) {
			System.out.println("dex->jar ʧ�� ");
			return false;
		}
		try {
			FileManager.unzip(jarPath, classesPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("��ѹ : " + jarPath + " -> " + classesPath);
		
		// ɾ����dex2jar���߰��е�classes.dex��classes-dex2jar.jar�ļ�
		FileManager.delete(dexTargetPath);
		System.out.println("ɾ�� : " + dexTargetPath);
		FileManager.delete(jarPath);
		System.out.println("ɾ�� : " + jarPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * ȡ�������class�ļ�������apk����Ŀ¼��patch�ļ���
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static boolean takeDifferenceClasses(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String sourceClassesPath = sourceUnzipPath + File.separator + "classes";
		String fixClassesPath = fixUnzipPath + File.separator + "classes";
		
		String patchClassesPath = patchUnzipPath + File.separator + "classes";
		FileManager.createDir(patchClassesPath);
		
		// ��fixApk��classes�ļ����Ƶ�patch�ļ���
		try {
			FileManager.copyDir(fixClassesPath, patchClassesPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("���� : " + fixClassesPath + " -> " + patchClassesPath);
		
		// ȥ����sourceApk��ͬ��Classes
		FileManager.compare(sourceClassesPath, patchClassesPath);
		System.out.println("�Ա� : " + sourceClassesPath + " <-> " + patchClassesPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * ��� classes -> jar -> dex
	 * @param patchUnzipPath
	 * @param dex2jarPath
	 */
	private static boolean packageClasses(String patchUnzipPath, String dex2jarPath) {
		
		String patchClassesPath = patchUnzipPath + File.separator + "classes";
		
		// �����޸ĵ�Class��jar��
		String classes2jarBatPath = dex2jarPath + File.separator + "classes2jar.bat";
		String patchJarPath = dex2jarPath + File.separator + "classes.jar";
		// cmd jar classes�ļ� ��� classes.jar�ļ�
		try {
			CMD.execute(classes2jarBatPath, patchClassesPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("��Jar�� : " + patchClassesPath + "=>" + patchJarPath);
		
		// cmd jar2dex classes.jar�ļ� ��� classes.dex�ļ�
		String patchDexPath = dex2jarPath + File.separator + "classes.dex";
		try {
			CMD.execute(dex2jarPath + File.separator + "jar2dex.bat");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("jar->dex : " + patchJarPath + "=>" + patchDexPath);
		
		// �� patch.dex ���Ƶ� ��apk��ͬĿ¼
		String patchDexTargetPath = patchUnzipPath + File.separator + "classes.dex";
		try {
			FileManager.copyFile(patchDexPath, patchDexTargetPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("���� : " + patchDexPath + " -> " + patchDexTargetPath);
		
		// ɾ��classes�ļ���
		FileManager.clear(patchClassesPath);
		System.out.println("ɾ�� : " + patchClassesPath);
		
		// ɾ����dex2jar���߰��е�classes.jar��classes.dex�ļ�
		FileManager.delete(patchJarPath);
		System.out.println("ɾ�� : " + patchJarPath);
		FileManager.delete(patchDexPath);
		System.out.println("ɾ�� : " + patchDexPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * ȡ�������res�ļ�������patch�ļ���
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static boolean takeDifferenceResource(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String sourceResPath = sourceUnzipPath + File.separator + "res";
		String fixResPath = fixUnzipPath + File.separator + "res";
		
		String patchResPath = patchUnzipPath + File.separator + "res";
		FileManager.createDir(patchResPath);
		
		// ��fixApk��res�ļ��и��Ƶ�patch�ļ���
		try {
			FileManager.copyDir(fixResPath, patchResPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("���� : " + fixResPath + " -> " + patchResPath);
		
		// ȥ����sourceApk��ͬ��Res
		FileManager.compare(sourceResPath, patchResPath);
		System.out.println("�Ա� : " + sourceResPath + " <-> " + patchResPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * ȡ�������lib�ļ�������patch�ļ���
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static boolean takeDifferenceLib(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		String sourceLibPath = sourceUnzipPath + File.separator + "lib";
		String fixLibPath = fixUnzipPath + File.separator + "lib";
		
		String patchLibPath = patchUnzipPath + File.separator + "lib";
		FileManager.createDir(patchLibPath);
		
		// ��fixApk��lib�ļ��и��Ƶ�patch�ļ���
		try {
			FileManager.copyDir(fixLibPath, patchLibPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("���� : " + fixLibPath + " -> " + patchLibPath);
		
		// ȥ����sourceApk��ͬ��Lib
		FileManager.compare(sourceLibPath, patchLibPath);
		System.out.println("�Ա� : " + sourceLibPath + " <-> " + patchLibPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * ɾ���յ��ļ���
	 * @return
	 */
	private static boolean clearEmpty(String patchUnzipPath) {
		
		try {
			String resPath = patchUnzipPath + File.separator + "res";
			FileManager.deleteEmptyDirectory(resPath);
			System.out.println("���  " + resPath + " ���ļ���");
			
			String libPath = patchUnzipPath + File.separator + "lib";
			FileManager.deleteEmptyDirectory(libPath);
			System.out.println("���  " + libPath + " ���ļ���");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		System.out.println("=======================================================================");
		
		return true;
	}
	
	/**
	 * ������ļ�֮��
	 * 1.���ԭʼ���޸���resources.arsc����ͬ������ resources.arsc
	 * 2.�������res�ļ��У����� resources.arsc
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
		
		System.out.println("�Ա� : " + sourceArscPath + " <-> " + fixArscPath + " = " + compareResult);
		
		if (!compareResult || patchResFile.exists()) {
			try {
				FileManager.copyFile(fixArscPath, patchArscPath);
				System.out.println("���� : " + fixArscPath + " -> " + patchArscPath);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		System.out.println("=======================================================================");
		
		return true;
	}
	
	/**
	 * ѹ�� 
	 * @param patchUnzipPath
	 */
	private static boolean compress(String patchUnzipPath) {
		
		File patchApk = new File(patchUnzipPath);
		String patchApkPath = patchApk.getParent() + File.separator + "patch.apk";
	
		// ѹ�� patch�ļ��� -> patch.apk
		FileManager.zip(patchUnzipPath, patchApkPath);
		System.out.println("ѹ�� : " + patchUnzipPath + " -> " + patchApkPath);
		
		System.out.println("=======================================================================");
		
		return true;
		
	}
	
	/**
	 * ������;���ɵ��ļ�
	 * @param sourceUnzipPath
	 * @param fixUnzipPath
	 * @param patchUnzipPath
	 */
	private static void clear(String sourceUnzipPath, String fixUnzipPath, String patchUnzipPath) {
		
		// ���ԭʼAPK���ɵ��ļ�
		FileManager.clear(sourceUnzipPath);
		System.out.println("���� : " + sourceUnzipPath);
		
		// ����޸�APK���ɵ��ļ�
		FileManager.clear(fixUnzipPath);
		System.out.println("���� : " + fixUnzipPath);
		
		// ���Patch�ļ�
		FileManager.clear(patchUnzipPath);
		System.out.println("���� : " + patchUnzipPath);
		
		System.out.println("===================================== ���� ==================================");
		
	}
	
}
