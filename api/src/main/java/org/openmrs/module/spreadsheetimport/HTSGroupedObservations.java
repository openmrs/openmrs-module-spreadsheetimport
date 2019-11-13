package org.openmrs.module.spreadsheetimport;

import java.util.Arrays;
import java.util.List;

public class HTSGroupedObservations implements DatasetGroupedObservations {

    public Integer getTemplateID() {
        return 10;
    }

    public List<GroupedObservations> getColumnDefinitions () {
        // build test 1 grouped obs
        GroupedObservations test1 = new GroupedObservations();
        test1.setGroupConceptId(164410);

        DatasetColumn testKitName = new DatasetColumn(164962, "value_coded");
        DatasetColumn testLotNo = new DatasetColumn(164964, "value_text");
        DatasetColumn testKitExpiry = new DatasetColumn(162502, "value_datetime");
        DatasetColumn test2KitExpiry = new DatasetColumn(162501, "value_datetime");
        DatasetColumn testResult = new DatasetColumn(1040, "value_coded");
        DatasetColumn test2Result = new DatasetColumn(1326, "value_coded");

        // add columns
        test1.addColumn("Test_1_Final_Result", testResult);
        test1.addColumn("Test_1_Kit_Name", testKitName);
        test1.addColumn("Test_1_Lot_Number", testLotNo);
        test1.addColumn("Test_1_Expiry_Date", testKitExpiry);


        // build test 2 grouped obs
        GroupedObservations test2 = new GroupedObservations();
        test2.setGroupConceptId(164410);


        // add columns
        test2.addColumn("Test_2_Final_Result", test2Result);
        test2.addColumn("Test_2_Kit_Name", testKitName);
        test2.addColumn("Test_2_Lot_Number", testLotNo);
        test2.addColumn("Test_2_Expiry_Date", test2KitExpiry);

        return Arrays.asList(test1, test2);
    }
}
