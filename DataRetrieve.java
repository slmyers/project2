import com.sleepycat.db.*;
import java.util.*;
import java.lang.Object;
import java.io.*;


public class DataRetrieve {
    //example key that has data: thndjefjyfwgpbmhzbfsfkubphiyvqirxwggmuxhvqnfmczshjaldffddmqyylwfmvbttcpvdjjffawzrdmwzykaspfugguntavetgdszamyogibkekcrvjuf

    //Example data values:
    //xeihommyihejcahcpnllqzsorhkervpimgxmfpqpaggerskevsnfcmdwjvtkulrxxckhxcavizjifgewprzxnkfgwdtaqltyxftdsymhmdyagsogpvhoc
    //hksbsenlqywqfnyephtunhyknikcpfnaxsvpngwiktgbmiyyjgqdauvbdbpdtixbekbfgrbnjtsafqlahbntvxybvnrfxehkiikmnuakxwwlxtcgpvhoc
    //dhjrxtadxewnvcpkhrgrbowprzblpslzszxhymcofszmagzbwdczkxyonshlmwroavtfpgigddzamcrbqisymyxnpwurpsbeekqvwviskwwlxtcgpvhoc
    //hvcthszqqdgppcwrcukjggolzfquilcxsssonsfsmgwvpuqthdyyvjsqidgiiyzssxptzmxilxocdqnysrjpetfejkzanpgxfhdubexahmsnfoqvuevfc
    //oedayeovsnubwvewkqwvdvvzticaluuzbcpabwxvsdszypbbxhsafxwftouhrmhowqozxsenbagzvfztlnhhlpnxrkojhscmmglitncrldmznkzcdwrpjaewdawvwe
    //cjfqwskqagppvgunbuuwjvehsytvyqvgmwxndniiebuccafpweohyzpzcpvpoltixalduhqyjktftelrhyrhbpnxrkojhscmmglitncrldmznkzcdwrpjaewdawvwe
    //cjfqwskqagppvgunbuuwjvehsytvyqvgmwxndniiebuccafpweohyzpzcpvpoltixalduhqyjktftelrhyrhbpnxrkojhscmmglitncrldmznkzcdwrpjaewdawvwe



    Scan scan = Scan.getInstance();
    Database database = null;
    Database db2 = null;
    String filename = "answers";
    WriteToFile fileWrite = new WriteToFile();
    Map<String, String> records = new HashMap<String, String>();

    public DataRetrieve(){

	DataBase db = DataBase.getInstance();
	

	if (Pref.getDbType() == 3) {
	    IndexFile index = IndexFile.getInstance();
	    index.configureDataSecondary();
	    db2 = index.getDataSecondary();

	}else{
	    database = db.getPrimaryDb();
	}	

    }

    //
    public void getRecords() {
	// gets user input for record to search for  
	System.out.print("Please enter data you want to search for: ");
	String searchData = scan.getString();
	
	// start timer, end before returns
	long timeStart = System.nanoTime();
	DatabaseEntry dbKey = new DatabaseEntry();
	DatabaseEntry pKey = new DatabaseEntry();
	
	//OperationStatus success = ;

	DatabaseEntry data = new DatabaseEntry();
	SecondaryDatabase database2 = null;

	String sData = null;
	String sKey = null;
		
	System.out.println("Searching for data in database");
	
	try {
	    //if there is a index file then we can use key search
	    if (Pref.getDbType() == 3) {

		Cursor c = db2.openCursor(null, null);
		//set dbKey to the data value we are searching for then move cursor
		dbKey.setData(searchData.getBytes());
		dbKey.setSize(searchData.length());
		if(c.getSearchKey(dbKey, data, LockMode.DEFAULT)==OperationStatus.SUCCESS){
		    sKey = new String (data.getData());
		    sData = new String (dbKey.getData());
		    data = new DatabaseEntry();
		    records.put(sKey, sData);

		    //next if there are duplicate keys after the first get them 		
		    while(c.getNext(dbKey, data, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			//if(sData == null){
			//    break;
			//}
			sData = new String (dbKey.getData());
			sKey = new String (data.getData());
			if(sData.equals(searchData)){
			    records.put(sKey, sData);
			}else{
			    break;
			}
		    }
		}
		c.close();
		
	    }else{    
		//if BTREE or HASH then search all records using cursor and return matches 
		Cursor c = database.openCursor(null, null);
		c.getFirst(dbKey, data, LockMode.DEFAULT);
	    	 
		for(int i = 0; i < 100000; i++){
		    sData = new String (data.getData());
		    sKey = new String (dbKey.getData());
		    data = new DatabaseEntry();
		
		    if(sData.equals(searchData)){
			records.put(sKey, sData);
		    }
		    c.getNext(dbKey, data, LockMode.DEFAULT);
		}
		c.close();
	    }
	    
	}
	catch (DatabaseException e) {
		System.err.println("unable to get key/value record!");
	}
	
	// end timer and print time
	long timeEnd = System.nanoTime();
	long time = timeEnd - timeStart;
	//time = time/1000000;
	System.out.println("TOTAL SEARCH TIME = " + time);
	System.out.println("Records found: "+ records.size());

	//get the key values of all records returned
	Set keys = records.keySet();
	Integer len = keys.size();
	String[] hashMapKeys = records.keySet().toArray(new String[len]);
	//iterate through hashMapKeys and write key/data pairs to answers.txt
	for(int i = 0; i < len; i++){
	    fileWrite.writeString(filename, hashMapKeys[i]); 
	    fileWrite.writeString(filename, records.get(hashMapKeys[i]));
	    fileWrite.writeString(filename, ""); 
    	 }
	return;
    }
  

}
