# Thesis
Android app, which reads sensors (Grove temperature sensor v1.2 + MQ-135 gas sensor) data from the BLE shield by RedBearLab mounted on Arduino Uno.
Later on, the data will be sent to 'SaMi' cloud service of Savonia UAS. 


Current implementation:
The sensors' data is persisted in the Room by background BLE service. ViewModel observes changes in the repository with help of LiveData. The UI subscribes to data changes in the ViewModel.
ViewPager in combination with FragmentPagerAdapter are used to build UI with graphs (using GraphView library). The fragments retain their state after configuration changes and reload previously observed data from the DB if the app is relaunched.
