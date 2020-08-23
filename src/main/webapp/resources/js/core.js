//$(document).bind("contextmenu",function(e){
//  return false;
//    });
//	
//	
//	document.onkeydown = function(e) {
//        if (e.ctrlKey && 
//            (e.keyCode === 67 || 
//             e.keyCode === 86 || 
//             e.keyCode === 85 || 
//             e.keyCode === 117)) {
//            
//            return false;
//        } else {
//            return false;
//        }
//};

$(document).ready(function(){
  $(".hamburger").click(function(){
    $(this).toggleClass("is-active");
  });
});