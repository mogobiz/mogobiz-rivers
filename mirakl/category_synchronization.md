# Category synchronization

| mirakl api | description | mogobiz requirements | mogobiz api | mogbiz model |
| --- | --- | --- | --- | --- |
| CA01 | Update categories from Operator Information System | should be applied on publishable categories only | `synchronizeCategories(RiverConfig config, List<MiraklCategory> categories):SynchronizationResponse` | Category.publishable: boolean |
| CA02 | Check the progress of the categories synchronization | should be applied on pending synchronization only | `refreshCategoriesSynchronizationStatus(RiverConfig config, Long importId):SynchronizationStatusResponse` | import_id: Long |
| CA03 | Get errors report file for categories synchronisation | should be applied on synchronization with errors only | `loadCategoriesSynchronizationErrorReport(RiverConfig config, Long importId):String` | -- |
