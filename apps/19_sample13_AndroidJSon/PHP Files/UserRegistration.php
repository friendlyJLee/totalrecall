<?php
if($_SERVER['REQUEST_METHOD']=='POST'){

include 'DatabaseConfig.php';

 $con = mysqli_connect($HostName,$HostUser,$HostPass,$DatabaseName);

 $F_name = $_POST['f_name'];
 $L_name = $_POST['L_name'];
 $email = $_POST['email'];
 $password = $_POST['password'];

 $CheckSQL = "SELECT * FROM UserLoginTable WHERE user_email='$email'";
 
 $check = mysqli_fetch_array(mysqli_query($con,$CheckSQL));
 
 if(isset($check)){

 echo 'Email Already Exist';

 }
else{ 
$Sql_Query = "INSERT INTO UserLoginTable (first_name,last_name,user_email,user_password) values ('$F_name','$L_name','$email','$password')";

 if(mysqli_query($con,$Sql_Query))
{
 echo 'Registration Successfully';
}
else
{
 echo 'Something went wrong';
 }
 }
}
 mysqli_close($con);
?>