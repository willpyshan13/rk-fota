<?php

/* 会上报这些参数，目前无用，若后续修改规则可能会用到
 $brand = $_GET["brand"];
 if(isset($brand))echo $brand;

 $model = $_GET["model"];
 if(isset($model))echo $model;

 $version = $_GET["version"];
 if(isset($version))echo $version;

 $fwtype = $_GET["fwtype"];
 if(isset($fwtype))echo $fwtype;
*/

 $filename = "OtaManifast.xml";

 $filepath = dirname(__FILE__)."/"; 


 if(file_exists($filepath.$filename))
 echo file_get_contents($filepath.$filename);
 
 // echo $str; //输出xml

?>
