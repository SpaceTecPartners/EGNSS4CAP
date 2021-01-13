$(document)
  .ready(function() {
  })
  .on('click', '.js_lang', function(){
    change_language($(this).attr('id'));
  })

function change_language(lang){
  $.ajax({
    type: 'POST',
    async: true,
    url: 'login.php',
    data: {
      act: 'lang_' + lang,
    }
  }).done(function(){
    location.reload();
  })
}