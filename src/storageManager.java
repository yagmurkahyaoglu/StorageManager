import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class Record {
	int id;
	String type;
	byte numOfFields;
	String[] nameOfFields;
	String[] fields;

	Record(String type, byte numOfFields){
		this.type = type;
		this.numOfFields = numOfFields;
		this.nameOfFields = new String[10];
		this.fields = new String[10];
	}
}

public class storageManager {

	public static void main(String args[]) throws IOException{

		String input = args[0];
		String output = args[1];
		Scanner in = new Scanner(new File(input));
		String operation = "";
		String typeOrRecord = "";
		ArrayList<String> outputs = new ArrayList<String>();

		while(in.hasNext()){

			operation = in.next();
			typeOrRecord = in.next();
			Record r;

			if(operation.equals("create")){
				if(typeOrRecord.equals("type")){
					String type = (in.next());
					int num = in.nextInt();
					r = new Record(type,(byte) num);
					for(int i=0; i<num; i++) r.nameOfFields[i] = in.next();
					createType(r);
				} else if(typeOrRecord.equals("record")){
					String type = (in.next());
					r = new Record(type,(byte) 0);
					int index = 0;
					while(in.hasNextInt()){
						r.fields[index] = in.nextInt() + "";
						index++;
					}
					createRecord(r);
				}
			} else if(operation.equals("delete")){
				if(typeOrRecord.equals("type")){
					String type = (in.next());
					r = new Record(type,(byte) 0);
					deleteType(r);
				} else if(typeOrRecord.equals("record")){
					String type = (in.next());
					r = new Record(type,(byte) 0);
					r.fields[0] = in.next();	
					deleteRecord(r);
				}
			} else if(operation.equals("list")){
				if(typeOrRecord.equals("type")){
					ArrayList<String> types = listType();
					for(String s : types) outputs.add(s);
				} else if(typeOrRecord.equals("record")){
					String type = (in.next());
					r = new Record(type,(byte) 0);
					ArrayList<String> records = listRecord(r);
					for(String s : records) outputs.add(s);
				}
			} else if(operation.equals("update")){
				String type = (in.next());
				r = new Record(type,(byte) 0);
				int index = 0;
				while(in.hasNextInt()){
					r.fields[index] = in.nextInt() + "";
					index++;
				}
				updateRecord(r);
			} else if(operation.equals("search")){
				String type = (in.next());
				r = new Record(type,(byte) 0);
				r.fields[0] = in.next();
				r = searchRecord(r);
				if(r != null){
					String s = "";
					for(int i=0; i<10; i++){
						if(r.fields[i] != null) s += r.fields[i] + " ";
						else break;
					}
					outputs.add(s);
				}

			}
		}
		in.close();

		FileWriter fileWriter = new FileWriter(output);
		for(String line : outputs) fileWriter.write(line + "\n");
		fileWriter.close();

	}

	public static void createType(Record type) throws IOException{
		RandomAccessFile cat = new RandomAccessFile("SystemCat.txt","rw");
		cat.seek(cat.length());
		String s = " ";
		s = fillTheBlanks(s,133);
		cat.write(s.getBytes());
		long pos = cat.length()-134;
		cat.seek(pos);
		cat.write(type.type.getBytes());
		pos += 12;
		cat.seek(pos);
		cat.writeByte(0);
		pos ++;
		cat.seek(pos);
		cat.writeByte(type.numOfFields);
		pos ++;
		cat.seek(pos);
		for(int i=0; i<type.numOfFields; i++){
			cat.write(type.nameOfFields[i].getBytes());
			pos += 12;
			cat.seek(pos);
		}
		cat.close();
		String file = type.type + "_1.txt";
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		dataFile.seek(0);
		s = " ";
		s = fillTheBlanks(s,29);
		dataFile.write(s.getBytes());
		dataFile.seek(0);
		dataFile.writeByte(1);
		dataFile.seek(1);
		dataFile.writeByte(0);
		dataFile.seek(2);
		dataFile.write(type.type.getBytes());
		dataFile.seek(14);
		file = type.type + "_2.txt";
		dataFile.write(file.getBytes());
		dataFile.close();

	}

	public static void deleteType(Record type) throws IOException{
		RandomAccessFile cat = new RandomAccessFile("SystemCat.txt","rw");
		boolean found = false;
		int pos = 0;
		while(!found){
			String currentType = "";
			byte[] bytes = new byte[12];
			cat.seek(pos);
			cat.read(bytes);
			currentType = new String(bytes);
			currentType = removeTheBlanks(currentType,12);
			if(currentType.equals(type.type)) found = true;
			else pos += 134;
		}
		pos ++;
		cat.writeByte(1);
		cat.close();
		String file = type.type + "_1.txt" ;
		int fileNum = 1;
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		while(dataFile.length() > 0){
			//dataFile.seek(1);
			//dataFile.writeByte(1);
			dataFile.close();
			Files.deleteIfExists(Paths.get(file));
			fileNum ++;
			file = type.type + "_" + fileNum + ".txt" ;
			dataFile = new RandomAccessFile(file,"rw");
		}
		dataFile.close();
	}

	public static ArrayList<String> listType() throws IOException{
		RandomAccessFile cat = new RandomAccessFile("SystemCat.txt","rw");
		ArrayList<String> types = new ArrayList<String>();
		int pos = 0;
		while(pos<cat.length()){
			String currentType = "";
			byte[] bytes = new byte[12];
			cat.seek(pos);
			cat.read(bytes);
			currentType = new String(bytes);
			cat.seek(pos+12);
			if(cat.readByte() == 0){
				currentType = removeTheBlanks(currentType,12);
				types.add(currentType);
			}
			pos += 134;
		}
		Collections.sort(types);
		cat.close();
		return types;
	}

	public static void createRecord(Record record) throws IOException{
		String file = record.type + "_1.txt" ;
		int fileNum = 1;
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		while(dataFile.length() == 17890){
			dataFile.close();
			fileNum ++;
			file = record.type + "_" + fileNum + ".txt" ;
			dataFile = new RandomAccessFile(file,"rw");
		}
		String s = " ";
		byte recordNum = 0;
		int pageId = 0;
		long fileLength = dataFile.length();
		if((fileLength-30)%1786 == 0){
			if(fileLength > 30){
				dataFile.seek(fileLength-1786);
				pageId = dataFile.readInt();
			} 
			else pageId = (fileNum-1)*10;
			dataFile.seek(fileLength);

			s = fillTheBlanks(s,5);
			dataFile.write(s.getBytes());
			dataFile.seek(fileLength);
			dataFile.writeInt(pageId+1);
			dataFile.seek(fileLength+4);
			dataFile.writeByte(0);
			dataFile.seek(fileLength+5);
			dataFile.writeByte(1);
		} else {
			dataFile.seek(fileLength-((fileLength-30)%1786));
			pageId = dataFile.readInt();
			dataFile.seek(fileLength-((fileLength-30)%1786)+5);
			recordNum = (byte) ((((fileLength-30)%1786)-6)%89);
			dataFile.writeByte(recordNum+1);
		}
		fileLength = dataFile.length();
		dataFile.seek(fileLength);
		s = " ";
		s = fillTheBlanks(s,88);
		dataFile.write(s.getBytes());
		dataFile.seek(fileLength);
		long recordId = (pageId-1)*200 + 1;
		dataFile.writeLong(recordId);
		dataFile.seek(fileLength+8);
		dataFile.writeByte(0);
		dataFile.seek(fileLength+9);
		for(int i=0; i<10; i++){
			if(record.fields[i] != null) dataFile.write(record.fields[i].getBytes());
			dataFile.seek(fileLength+9+(i+1)*8);
		}
		dataFile.close();	
	}

	public static void deleteRecord(Record record)throws IOException{
		boolean found = false;
		long pos = 0;
		String file = record.type + "_1.txt" ;
		int fileNum = 1;
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		while(!found && dataFile.length() > 0){
			pos = searchPos(dataFile, record);
			if(pos != -1) break;
			dataFile.close();
			fileNum ++;
			file = record.type + "_" + fileNum + ".txt" ;
			dataFile = new RandomAccessFile(file,"rw");
		}
		dataFile.seek(pos+8);
		dataFile.writeByte(1);
		boolean isAllRecDeleted = true;
		pos = ((pos-30)/1786)*1786 + 36;
		dataFile.seek(pos);
		for(int i=0; i<20; i++){
			dataFile.seek(pos+8);
			if(dataFile.readByte()==0) isAllRecDeleted = false;
			pos += 89;
			if(pos >= dataFile.length()) break;
		}
		boolean isAllPageDeleted = true;
		if(isAllRecDeleted){
			pos = ((pos-30)/1786)*1786 + 30;
			dataFile.seek(pos+4);
			dataFile.writeByte(1);
			pos = 30;
			for(int i=0; i<10; i++){
				dataFile.seek(pos+4);
				if(dataFile.readByte()==0) isAllPageDeleted = false;
				pos += 1786;
				if(pos >= dataFile.length()) break;
			}
			if(isAllPageDeleted){
				dataFile.seek(1);
				dataFile.writeByte(1);
			}
		}
		dataFile.close();
	}

	public static void updateRecord(Record record)throws IOException{
		boolean found = false;
		long pos = 0;
		String file = record.type + "_1.txt" ;
		int fileNum = 1;
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		while(!found && dataFile.length() > 0){
			pos = searchPos(dataFile, record);
			if(pos != -1) break;
			dataFile.close();
			fileNum ++;
			file = record.type + "_" + fileNum + ".txt" ;
			dataFile = new RandomAccessFile(file,"rw");
		}
		dataFile.seek(pos+17);
		for(int i=1; i<10; i++){
			if(record.fields[i] != null) dataFile.write(record.fields[i].getBytes());
			dataFile.seek(pos+17+(i)*8);
		}
		dataFile.close();
	}

	public static Record searchRecord(Record record)throws IOException{
		boolean found = false;
		long pos = -1;
		String file = record.type + "_1.txt" ;
		int fileNum = 1;
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		while(!found && dataFile.length() > 0){
			pos = searchPos(dataFile, record);
			dataFile.seek(1);
			if(pos != -1 && dataFile.readByte()==0) break;
			dataFile.close();
			fileNum ++;
			file = record.type + "_" + fileNum + ".txt" ;
			dataFile = new RandomAccessFile(file,"rw");
		}
		if(pos == -1) return null;
		else {
			dataFile.seek(pos+8);
			if(dataFile.readByte()==1) return null;
			else {
				dataFile.seek(pos+9);
				Record r = new Record(record.type,(byte) 0);
				for(int i=0; i<10; i++){
					String field = "";
					byte[] bytes = new byte[8];
					dataFile.read(bytes);
					field = new String(bytes);
					field = removeTheBlanks(field,8);
					r.fields[i] = field;
					dataFile.seek(pos+9+(i+1)*8);
				}
				return r;
			}
		}
	}

	public static ArrayList<String> listRecord(Record record) throws IOException{
		String file = record.type + "_1.txt" ;
		int fileNum = 1;
		RandomAccessFile dataFile = new RandomAccessFile(file,"rw");
		ArrayList<String> records = new ArrayList<String>();
		while(dataFile.length()>0){
			dataFile.seek(1);
			if(dataFile.readByte()==0){
				long length = 30;
				while(length < dataFile.length()){
					dataFile.seek(length+4);
					if(dataFile.readByte()==0){
						dataFile.seek(length+6);
						int recordNum = 1;
						while(recordNum <= 10){
							if(length+6+recordNum*89 <= dataFile.length()){
								long pos = length+6+(recordNum-1)*89;
								dataFile.seek(pos+8);
								if(dataFile.readByte()==0){
									dataFile.seek(pos+9);
									Record r = new Record(record.type,(byte) 0);
									for(int i=0; i<10; i++){
										String field = "";
										byte[] bytes = new byte[8];
										dataFile.read(bytes);
										field = new String(bytes);
										field = removeTheBlanks(field,8);
										r.fields[i] = field;
										dataFile.seek(pos+9+(i+1)*8);
									}
									String s = "";
									for(int j=0; j<10; j++){
										if(r.fields[j] != null) s += r.fields[j] + " ";
										else break;
									}
									records.add(s);
								}

							}
							recordNum++;
						}
					}
					length += 1786;
				}
			}
			dataFile.close();
			fileNum ++;
			file = record.type + "_" + fileNum + ".txt" ;
			dataFile = new RandomAccessFile(file,"rw");
		}
		dataFile.close();
		Collections.sort(records);
		return records;

	}

	public static long searchPos(RandomAccessFile file, Record record) throws IOException{
		long pagePos = 30;
		long recordPos = 0;
		file.seek(1);
		if(file.readByte()==0){
			while(pagePos < file.length()){
				file.seek(pagePos + 4);
				if(file.readByte()==0){
					recordPos = pagePos + 6;
					while(recordPos < pagePos + 1786){
						byte isDeleted = 0;
						file.seek(recordPos+8);
						isDeleted = file.readByte();
						file.seek(recordPos+9);
						String currentType = "";
						byte[] bytes = new byte[8];
						file.read(bytes);
						currentType = new String(bytes);
						currentType = removeTheBlanks(currentType,8);
						if(currentType.equals(record.fields[0])){
							if(isDeleted == 1) return -1;
							else return recordPos;
						}
						recordPos += 89;
					}
					pagePos += 1786;
				} else {
					pagePos += 1786;
					continue;
				}
			}
		}
		return -1;
	}

	public static String fillTheBlanks(String s, int n){
		for(int i=0; i<n; i++) s += " ";
		return s;
	}

	public static String removeTheBlanks(String s,int n){
		if(n==8 && s.equals("        ")) return "";
		for (int i=n-1; i>0; i--){
			if(s.charAt(i) == ' ' && s.charAt(i-1) != ' '){
				s = s.substring(0,i);
				break;
			}
		}
		return s;
	}

}
