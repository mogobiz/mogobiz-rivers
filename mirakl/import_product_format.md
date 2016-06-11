# Import Product format

## Import Product Hierachy

| mirakl api | description | mogobiz requirements | mogobiz api | mogbiz model |
| -- | -- | -- | -- | -- |
| H01 | Import the CSV file describing what actions to take on the hierarchy | should be applied on publishable categories only | `importHierarchies(RiverConfig config, List<MiraklHierarchy> hierarchies):ImportResponse` | Category.publishable: boolean |
| H02 | Check the progress of the product hierarchy synchronization | should be applied on pending synchronization only | `trackHierarchiesImportStatusResponse(RiverConfig config, Long importId):ImportStatusResponse` | import_id: Long|
| H03 | Get errors report file for hierarchies synchronisation | should be applied on synchronization with errors only | `loadHierarchiesSynchronizationErrorReport(RiverConfig config, Long importId):String` | -- |

## Import List of Values

| mirakl api | description | mogobiz requirements | mogobiz api | mogbiz model |
| -- | -- | -- | -- | -- |
| VL01 | Import the CSV file describing the lists of values | should be applied on publishable product features and variations | `importValues(RiverConfig config, List<MiraklValue> values = []):ImportResponse` | Product.publishable: boolean |
| VL02 | Check the progress of the product values synchronization  | should be applied on pending synchronization only | `trackValuesImportStatusResponse(RiverConfig config, Long importId):ImportStatusResponse` | import_id: Long |
| VL03 | Get errors report file for product values synchronisation | should be applied on synchronization with errors only | `loadValuesSynchronizationErrorReport(RiverConfig config, Long importId):String` | -- |

## Import Attributes

| mirakl api | description | mogobiz requirements | mogobiz api | mogbiz model |
| -- | -- | -- | -- | -- |
| PM01 | Import the CSV file describing the product attributes | -- | `importAttributes(RiverConfig config, List<MiraklAttribute> attributes = []):ImportResponse` | via json file ? |
| PM02 | Check the progress of the product attributes synchronization  | should be applied on pending synchronization only | `trackAttributesImportStatusResponse(RiverConfig config, Long importId):ImportStatusResponse` | import_id: Long |
| PM03 | Get errors report file for product attributes synchronisation | should be applied on synchronization with errors only | `loadAttributesSynchronizationErrorReport(RiverConfig config, Long importId):String` | -- |


