CREATE TABLE `TAuthorityLevel` (
  `ItemName` varchar(80) DEFAULT NULL,
  `level` smallint(5) unsigned DEFAULT NULL,
  `rowID` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`rowID`)
);
INSERT INTO `TAuthorityLevel` VALUES ('SuperMan',1,1),('Super Admin',20,10000),('Admin',50,10001),('User',100,10002);
CREATE TABLE `TClassFile` (
  `fileID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `fileName` char(80) DEFAULT NULL,
  `fileBody` longblob,
  `fileType` varchar(20) DEFAULT NULL,
  `fileVersion` bigint(20) DEFAULT NULL,
  `fileDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fileID`)
);
CREATE TABLE `TComputerDetail` (
  `hostID` bigint(20) unsigned NOT NULL,
  `detailInformation` longblob,
  `status` int(11) DEFAULT NULL,
  `version` tinyint(1) DEFAULT '-1',
  `osType` tinyint(1) DEFAULT NULL,
  `dateAdded` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE `TComputerInformation` (
  `hostID` bigint(20) unsigned DEFAULT NULL,
  `itemName` varchar(80) DEFAULT NULL,
  `itemField` varchar(80) DEFAULT NULL,
  `itemValue` varchar(200) DEFAULT NULL,
  `version` tinyint(1) DEFAULT '-1',
  `status` int(11) DEFAULT NULL,
  `deviceID` tinyint(3) DEFAULT NULL,
  `osType` tinyint(1) DEFAULT NULL,
  KEY `hostID` (`hostID`),
  KEY `hostID_2` (`hostID`)
);
CREATE TABLE `TFileKey` (
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `fileKey` blob,
  PRIMARY KEY (`rowID`)
);
CREATE TABLE `TGServerInfo` (
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serverID` int(10) unsigned DEFAULT NULL,
  `serverName` varchar(50) DEFAULT NULL,
  `installedDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `serverIP1` varchar(39) DEFAULT NULL,
  `serverMAC1` varchar(17) DEFAULT NULL,
  `serverDomain` varchar(50) DEFAULT NULL,
  `serverIP2` varchar(39) DEFAULT NULL,
  `serverIP3` varchar(39) DEFAULT NULL,
  `serverIP4` varchar(39) DEFAULT NULL,
  `serverCountry` varchar(50) DEFAULT NULL,
  `serverMAC2` varchar(17) DEFAULT NULL,
  `serverMAC3` varchar(17) DEFAULT NULL,
  `serverMAC4` varchar(17) DEFAULT NULL,
  `companyName` varchar(80) DEFAULT NULL,
  `contact` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `securityKey1` blob,
  `securityKey2` varchar(64) DEFAULT NULL,
  `outPort` int(10) unsigned DEFAULT NULL,
  `inPort` int(10) unsigned DEFAULT NULL,
  `idCode` varchar(5) DEFAULT NULL,
  `serverLevel` smallint(5) unsigned DEFAULT '100',
  `serverRole` smallint(5) unsigned DEFAULT NULL,
  PRIMARY KEY (`rowID`)
);
CREATE TABLE `TGroup` (
  `userID` bigint(20) unsigned NOT NULL,
  `DepartmentID` int(10) unsigned DEFAULT NULL,
  `CompanyID` int(10) unsigned DEFAULT NULL,
  `AgencyID` int(10) unsigned DEFAULT NULL,
  `titleID` int(10) unsigned DEFAULT NULL,
  `userLocation` varchar(80) DEFAULT NULL,
  `userManager` varchar(80) DEFAULT NULL,
  `officePhone` varchar(20) DEFAULT NULL,
  `faxPhone` varchar(20) DEFAULT NULL,
  `officeAddress` varchar(80) DEFAULT NULL,
  `officeCity` varchar(80) DEFAULT NULL,
  `officeState` varchar(80) DEFAULT NULL,
  `officeCountry` varchar(80) DEFAULT NULL,
  `officeZip` varchar(15) DEFAULT NULL,
  `hostID` int(10) unsigned DEFAULT NULL,
  `assistant` varchar(80) DEFAULT NULL,
  `userName` varchar(50) DEFAULT NULL,
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `userLevel` tinyint(4) unsigned DEFAULT '100',
  `owners` bigint(20) unsigned DEFAULT NULL,
  `editorID` bigint(20) unsigned DEFAULT NULL,
  `lastUpdated` bigint(20) unsigned DEFAULT NULL,
  `ServerID` int(10) unsigned NOT NULL,
  `isEdit` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`rowID`),
  KEY `userID` (`userID`),
  KEY `serverID` (`ServerID`)
);
CREATE TABLE `THostInformation` (
  `hostID` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `firstSeen` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `singleComputer` tinyint(1) DEFAULT NULL,
  `MACAddress` varchar(30) NOT NULL,
  `done` tinyint(1) DEFAULT '0',
  `trustIT` tinyint(1) DEFAULT '0',
  `IPAddress` varchar(40) DEFAULT NULL,
  `computerName` varchar(80) DEFAULT NULL,
  `OS` varchar(50) DEFAULT NULL,
  `isLogon` tinyint(1) DEFAULT '0',
  `comments` varchar(100) DEFAULT NULL,
  `warranty` varchar(50) DEFAULT NULL,
  `userName` varchar(50) DEFAULT NULL,
  `ProductID` varchar(50) DEFAULT NULL,
  `AgencyID` varchar(5) DEFAULT NULL,
  `CompanyID` varchar(5) DEFAULT NULL,
  `DepartmentID` varchar(5) DEFAULT NULL,
  `isNormal` tinyint(1) NOT NULL DEFAULT '1',
  `lastLogin` bigint(20) DEFAULT '0',
  PRIMARY KEY (`hostID`),
  KEY `hostID` (`hostID`)
);
CREATE TABLE `THostofUser` (
  `userID` bigint(20) unsigned DEFAULT NULL,
  `hostID` bigint(20) unsigned DEFAULT NULL,
  `joinTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `serverID` int(10) unsigned DEFAULT NULL,
  `serverofHost` int(10) unsigned DEFAULT NULL,
  KEY `userID` (`userID`),
  KEY `serverID` (`serverID`)
);
CREATE TABLE `TICSKey` (
  `interactionKey` tinyblob,
  `createTime` bigint(20) DEFAULT NULL
);
CREATE TABLE `TLServerInfo` (
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serverID` int(10) unsigned DEFAULT NULL,
  `serverName` varchar(50) DEFAULT NULL,
  `installedDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `serverIP1` varchar(39) DEFAULT NULL,
  `serverMAC1` varchar(17) DEFAULT NULL,
  `serverDomain` varchar(50) DEFAULT NULL,
  `serverIP2` varchar(39) DEFAULT NULL,
  `serverIP3` varchar(39) DEFAULT NULL,
  `serverIP4` varchar(39) DEFAULT NULL,
  `serverCountry` varchar(50) DEFAULT NULL,
  `serverMAC2` varchar(17) DEFAULT NULL,
  `serverMAC3` varchar(17) DEFAULT NULL,
  `serverMAC4` varchar(17) DEFAULT NULL,
  `companyName` varchar(80) DEFAULT NULL,
  `contact` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `securityKey1` blob,
  `securityKey2` varchar(64) DEFAULT NULL,
  `outPort` int(10) unsigned DEFAULT NULL,
  `inPort` int(10) unsigned DEFAULT NULL,
  `idCode` varchar(5) DEFAULT NULL,
  `serverRole` smallint(5) unsigned DEFAULT NULL,
  `serverLevel` smallint(5) unsigned DEFAULT NULL,
  PRIMARY KEY (`rowID`)
);
CREATE TABLE `TLevelInformation` (
  `levelName` varchar(50) DEFAULT NULL,
  `levelNumber` smallint(5) unsigned DEFAULT NULL,
  `Comments` varchar(100) DEFAULT NULL,
  `createUser` tinyint(1) DEFAULT '0',
  `createGroup` tinyint(1) DEFAULT '0',
  `createOrgnization` tinyint(1) DEFAULT '0',
  `editUser` tinyint(1) DEFAULT '0',
  `editGroup` tinyint(1) DEFAULT '0',
  `editOrgnization` tinyint(1) DEFAULT '0',
  `levelID` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `isEdit` tinyint(1) DEFAULT '0',
  `editorID` bigint(20) unsigned DEFAULT NULL,
  `controlHost` tinyint(1) DEFAULT '0',
  `createAgency` tinyint(1) DEFAULT '0',
  `editAgency` tinyint(1) DEFAULT '0',
  `createLevel` tinyint(1) DEFAULT '0',
  `editLevel` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`levelID`),
  UNIQUE KEY `level` (`levelNumber`)
);
CREATE TABLE `TLoginInfo` (
  `userID` bigint(20) unsigned DEFAULT NULL,
  `hostID` bigint(20) unsigned DEFAULT NULL,
  `loginTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `loginDone` tinyint(1) DEFAULT '0'
);
CREATE TABLE `TOrgInformation` (
  `orgName` varchar(80) NOT NULL,
  `pCode` int(10) unsigned DEFAULT NULL,
  `idCode` int(10) unsigned DEFAULT NULL,
  `officePhone` varchar(15) DEFAULT NULL,
  `faxPhone` varchar(15) DEFAULT NULL,
  `streetAddress` varchar(80) DEFAULT NULL,
  `city` varchar(20) DEFAULT NULL,
  `stateProvince` varchar(20) DEFAULT NULL,
  `zipPostal` varchar(10) DEFAULT NULL,
  `countryRegion` varchar(30) DEFAULT NULL,
  `isEdit` tinyint(1) DEFAULT '0',
  `editorID` bigint(20) unsigned DEFAULT NULL,
  `Owners` int(10) unsigned DEFAULT NULL,
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serverID` int(10) unsigned DEFAULT NULL,
  `serverName` varchar(50) DEFAULT NULL,
  `orgType` char(1) NOT NULL DEFAULT 'D',
  `description` varchar(255) DEFAULT NULL,
  `isNew` tinyint(1) NOT NULL DEFAULT '1',
  `aCode` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`rowID`),
  UNIQUE KEY `idCode` (`idCode`)
);
CREATE TABLE `TSecurityKey` (
  `keyID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `securityLocker` blob,
  `hostID` bigint(20) unsigned DEFAULT NULL,
  `singleKey` tinyint(1) DEFAULT '0',
  `singleComputer` tinyint(1) DEFAULT '0',
  `needUpdate` tinyint(1) DEFAULT '0',
  `disabled` tinyint(1) DEFAULT '0',
  `toIDFile` tinyint(1) DEFAULT '0',
  `seckey2` varchar(64) DEFAULT NULL,
  `done` tinyint(1) DEFAULT '0',
  `updateTimes` int(10) unsigned DEFAULT NULL,
  `decodeKey` tinyblob,
  `decodeKey2` bigint(20) unsigned DEFAULT NULL,
  `secKey2b` varchar(64) DEFAULT NULL,
  `isLocked` tinyint(1) DEFAULT '0',
  `loginTimes` tinyint(1) DEFAULT '0',
  `lockTime` bigint(20) DEFAULT NULL,
  `userID` bigint(20) unsigned DEFAULT '0',
  `userKey` tinyint(1) DEFAULT '0',
  `MACAddress` varchar(30) NOT NULL,
  PRIMARY KEY (`keyID`),
  KEY `keyID` (`keyID`)
);
CREATE TABLE `TTitle` (
  `titleName` varchar(50) DEFAULT NULL,
  `titleID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `departmentID` int(10) unsigned DEFAULT NULL,
  `editorID` bigint(20) unsigned DEFAULT NULL,
  `isEdit` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`titleID`)
);
CREATE TABLE `TUserGroup` (
  `groupID` int(10) unsigned DEFAULT NULL,
  `userID` bigint(20) unsigned DEFAULT NULL,
  `isUser` tinyint(1) DEFAULT '0',
  `DepartmentID` char(5) DEFAULT NULL,
  `userName` varchar(50) DEFAULT NULL,
  `serverID` int(10) unsigned DEFAULT NULL,
  KEY `indexuser` (`userID`)
);
CREATE TABLE `TUserInformation` (
  `userName` varchar(50) NOT NULL,
  `userSex` varchar(1) NOT NULL DEFAULT 'M',
  `userAge` tinyint(4) DEFAULT NULL,
  `joinDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `hostID` bigint(20) unsigned DEFAULT NULL,
  `isLocked` tinyint(1) DEFAULT '0',
  `userLevel` smallint(5) NOT NULL DEFAULT '100',
  `chineseName` varchar(50) DEFAULT NULL,
  `firstName` varchar(50) DEFAULT NULL,
  `middleName` varchar(50) DEFAULT NULL,
  `lastName` varchar(50) DEFAULT NULL,
  `birthday` bigint(20) unsigned DEFAULT '0',
  `idCard` varchar(20) DEFAULT NULL,
  `singleKey` tinyint(1) DEFAULT '0',
  `keyID` int(10) unsigned DEFAULT NULL,
  `loginTimes` tinyint(1) DEFAULT '0',
  `lockTime` bigint(20) DEFAULT NULL,
  `isLogon` tinyint(1) DEFAULT '0',
  `singleLogon` tinyint(1) DEFAULT '0',
  `softwareLevel` varchar(1024) NOT NULL DEFAULT '1111111110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
  `forceSecurity` tinyint(1) DEFAULT '0',
  `forceLevel` tinyint(1) DEFAULT '3',
  `isEdit` tinyint(1) DEFAULT '1',
  `editorID` bigint(20) unsigned DEFAULT NULL,
  `emailAddress` varchar(80) DEFAULT NULL,
  `homeCity` varchar(80) DEFAULT NULL,
  `homeCountry` varchar(80) DEFAULT NULL,
  `homeZip` varchar(15) DEFAULT NULL,
  `homeState` varchar(80) DEFAULT NULL,
  `officePhone` varchar(80) DEFAULT NULL,
  `userMobile` varchar(20) DEFAULT NULL,
  `isDisabled` tinyint(1) DEFAULT '0',
  `userID` bigint(20) unsigned DEFAULT NULL,
  `owners` bigint(20) unsigned DEFAULT NULL,
  `lastUpdated` bigint(20) unsigned DEFAULT NULL,
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serverID` int(10) unsigned NOT NULL,
  `inetAddress` varchar(80) DEFAULT NULL,
  `secKey` blob,
  `limitsize` tinyint(4) DEFAULT '32',
  `description` varchar(255) DEFAULT NULL,
  `expired` bigint(20) unsigned DEFAULT '2524579200000',
  `userPassword` varchar(80) NOT NULL DEFAULT 'Passw0rd',
  `passwordNext` tinyint(1) unsigned DEFAULT '1',
  `passwordExpired` tinyint(1) DEFAULT '0',
  `isNew` tinyint(1) NOT NULL DEFAULT '1',
  `lastLogin` bigint(20) DEFAULT '0',
  PRIMARY KEY (`rowID`),
  KEY `userID` (`userID`)
);
CREATE TABLE `TUserProf` (
  `rowID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `userID` bigint(20) unsigned DEFAULT NULL,
  `headerImage` blob,
  `defaultFolder` varchar(255) DEFAULT NULL,
  `nickName` varchar(50) DEFAULT NULL,
  `mobile` bigint(20) unsigned DEFAULT NULL,
  `iconImage` blob,
  `iImageNo` int(10) unsigned DEFAULT '0',
  `nickNameNo` int(10) unsigned DEFAULT '0',
  `profVersion` bigint(20) DEFAULT '0',
  `diskQuote` bigint(20) DEFAULT '1024',
  `usedQuote` bigint(20) DEFAULT '1024',
  PRIMARY KEY (`rowID`),
  UNIQUE KEY `userID` (`userID`)
);
create VIEW `VHost` AS select `THostInformation`.`hostID` AS `hostID`,`THostInformation`.`computerName` AS `computerName`,`THostInformation`.`OS` AS `OS`,`THostInformation`.`IPAddress` AS `IPAddress`,`THostInformation`.`trustIT` AS `trustIT`,`THostInformation`.`userName` AS `userName`,`TSecurityKey`.`disabled` AS `disabled`,`TSecurityKey`.`isLocked` AS `isLocked`,`THostInformation`.`isLogon` AS `isLogon`,`THostInformation`.`CompanyID` AS `CompanyID`,`THostInformation`.`DepartmentID` AS `DepartmentID`,`THostInformation`.`comments` AS `comments` from (`THostInformation` left join `TSecurityKey` on((`TSecurityKey`.`hostID` = `THostInformation`.`hostID`)));
create VIEW `VUser` AS select `TGroup`.`rowID` AS `rowID`,`TUserInformation`.`isDisabled` AS `isDisabled`,`TUserInformation`.`loginTimes` AS `loginTimes`,`TUserInformation`.`lockTime` AS `lockTime`,`TUserInformation`.`userPassword` AS `userPassword`,`TUserInformation`.`chineseName` AS `chineseName`,`TUserInformation`.`limitsize` AS `limitsize`,`TUserInformation`.`isLogon` AS `isLogon`,`TUserInformation`.`singleLogon` AS `singleLogon`,`TUserInformation`.`singleKey` AS `singleKey`,`TUserInformation`.`isLocked` AS `isLocked`,`TUserInformation`.`expired` AS `expired`,`TUserInformation`.`passwordNext` AS `passwordNext`,`TUserInformation`.`passwordExpired` AS `passwordExpired`,`TUserInformation`.`hostID` AS `hostID`,`TUserInformation`.`owners` AS `owners`,`TGroup`.`userManager` AS `userManager`,`TUserInformation`.`softwareLevel` AS `softwareLevel`,(select `TAuthorityLevel`.`ItemName` from `TAuthorityLevel` where (`TAuthorityLevel`.`level` = `TUserInformation`.`userLevel`)) AS `LevelName`,`TGroup`.`ServerID` AS `serverID`,`TUserInformation`.`userID` AS `userID`,`TUserInformation`.`userLevel` AS `userLevel`,`TGroup`.`AgencyID` AS `AgencyID`,`TGroup`.`CompanyID` AS `CompanyID`,`TGroup`.`DepartmentID` AS `DepartmentID`,`TGroup`.`userName` AS `userName`,(select `TTitle`.`titleName` from `TTitle` where (`TGroup`.`titleID` = `TTitle`.`titleID`)) AS `titleName` from (`TGroup` left join `TUserInformation` on((`TGroup`.`userID` = `TUserInformation`.`userID`))) where (`TUserInformation`.`isNew` = 0);




