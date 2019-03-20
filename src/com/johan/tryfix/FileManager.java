package com.johan.tryfix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

public class FileManager {

	/**
	 * 文件重命名
	 * @param oldPath
	 * @param newPath
	 */
	public static void rename(String oldPath, String newPath) {
		File oldFile = new File(oldPath);
		File newFile = new File(newPath);
		oldFile.renameTo(newFile);
	}
	
	/**
	 * 解压
	 * @param sourceZip
	 * @param destDir
	 * @throws Exception
	 */
	public static void unzip(String sourceZip,String destDir) throws Exception{    
		Project project = new Project();
		Expand expand = new Expand();
		expand.setProject(project);
		expand.setSrc(new File(sourceZip));
		expand.setOverwrite(true);
		expand.setDest(new File(destDir));
		expand.setEncoding("gbk");
		expand.execute();
	}
	
	/**
     * 压缩
     * @param sourceFile
     * @param destZip
     */
    public static void zip(String sourceFile, String destZip) {
        Project project = new Project();
        Zip zip = new Zip();
        zip.setProject(project);
        zip.setDestFile(new File(destZip));
        FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        File file = new File(sourceFile);
        if (file.isDirectory()) {
            fileSet.setDir(file);
        } else {
            fileSet.setFile(file);
        }
        zip.addFileset(fileSet);
        zip.setEncoding("gbk");
        zip.execute();
    }
	
	/**
	 * 复制文件
	 * @param sourcePath
	 * @param targetPath
	 * @throws IOException
	 */
	public static void copyFile(String sourcePath, String targetPath) throws IOException {
		File source = new File(sourcePath);
		File target = new File(targetPath);
		if (target.exists()) {
			target.delete();
		}
		Files.copy(source.toPath(), target.toPath());
	}
	
	/**
	 * 复制文件夹
	 * @param sourcePath
	 * @param targetPath
	 * @throws IOException
	 */
	public static void copyDir(String sourcePath, String targetPath) throws IOException {
        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
			targetFile.mkdirs();
		}
        File[] sourceFiles = sourceFile.listFiles();
        for (File file : sourceFiles) {
        	if (file.isFile()) {
				copyFile(file.getAbsolutePath(), targetPath + File.separator + file.getName());
        	}
        	if (file.isDirectory()) {
        		copyDir(file.getAbsolutePath(), targetPath + File.separator + file.getName());
			}
        }
    }
	
	/**
	 * 判断文件是否存在
	 * @param path
	 * @return
	 */
	public static boolean isExist(String path) {
		File file = new File(path);
		return file.exists();
	}
	
	/**
	 * 删除文件
	 * @param path
	 * @return
	 */
	public static void delete(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
	}
	
	/**
	 * 清空文件夹
	 * @param path
	 */
	public static void clear(String path) {
		File directory = new File(path);
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.isFile()) {
					boolean result = file.delete();
					while (!result) {
						result = file.delete();
					}
				}
				if (file.isDirectory()) {
					clear(file.getAbsolutePath());
				}
			}
			boolean result = directory.delete();
			while (!result) {
				result = directory.delete();
			}
		}
	}
	
	/**
	 * 删除空的文件夹
	 * @param path
	 */
	public static void deleteEmptyDirectory(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files == null || files.length == 0) {
				file.delete();
				return;
			}
			for (File childFile : files) {
				if (childFile.isDirectory()) {
					deleteEmptyDirectory(childFile.getAbsolutePath());
				}
			}
			if (file.listFiles().length == 0) {
				file.delete();
			}
		}
	}
	
	/**
	 * 创建文件夹
	 * @param path
	 */
	public static void createDir(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
		file.mkdirs();
	}
	
	/**
	 * 对比文件
	 * @param sourcePath
	 * @param patchPath
	 */
	public static void compare(String sourcePath, String patchPath) {
		File sourceDirectory = new File(sourcePath);
		File[] sourceFiles = sourceDirectory.listFiles();
		for (File sourceFile : sourceFiles) {
			File targetFile = new File(patchPath + File.separator + sourceFile.getName());
			if (sourceFile.isFile()) {
				if (!targetFile.exists()) {
					continue;
				}
				if (targetFile.getName().equals("R.class")) {
					System.out.println("保持 : " + sourceFile.getPath());
					continue;
				}
				boolean compareResult = FileManager.isSame(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
				if (compareResult) {
					targetFile.delete();
				} else {
					System.out.println("修改 : " + sourceFile.getPath());
				}
				continue;
			}
			if (sourceFile.isDirectory()) {
				compare(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
			}
		}
	}
	
	/**
	 * 对比两个文件是否相同
	 * @param file1
	 * @param file2
	 * @return
	 */
	public static boolean isSame(String path1, String path2) {
		File file1 = new File(path1);
		File file2 = new File(path2);
		if (file1.length() != file2.length()) {
			return false;
		}
		return getFileMD5(file1).equals(getFileMD5(file2));
	}
	
	/**
	 * 获取File的MD5
	 * @param file
	 * @return
	 */
	public static String getFileMD5(File file) {
	    if (!file.isFile()) {
	        return null;
	    }
	    MessageDigest digest = null;
	    FileInputStream inputStream = null;
	    byte buffer[] = new byte[8192];
	    int len;
	    try {
	        digest =MessageDigest.getInstance("MD5");
	        inputStream = new FileInputStream(file);
	        while ((len = inputStream.read(buffer)) != -1) {
	            digest.update(buffer, 0, len);
	        }
	        BigInteger bigInt = new BigInteger(1, digest.digest());
	        return bigInt.toString(16);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    } finally {
	        try {
	            inputStream.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
	
}
