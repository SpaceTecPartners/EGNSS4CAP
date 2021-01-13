<?php
namespace task;

class task_view{

  protected $TEMPLATES_PATH;
  protected $JS_PATH;
  protected $CSS_PATH;
  public const TASK_PHOTO = 4;
  public const TASK_DETAIL = 5;
  public const PHOTO_PDF_EXPORT = 91;

  private const MY_PATH = 'task' . DIRECTORY_SEPARATOR;

  private $twig;
  public function __construct($path = ''){
    $this->setTwig(new \twig_controler());
    $this->setTEMPLATES_PATH(self::MY_PATH.$path);
    $this->setJS_PATH('js' . DIRECTORY_SEPARATOR . self::MY_PATH . $path);
    $this->setCSS_PATH('css' . DIRECTORY_SEPARATOR . self::MY_PATH . $path);
  }

  public function load_task_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'task.js');
    $twig->use_map_js();
    $twig->pushCss($this->getCSS_PATH() . 'task.css');
    $this->getTwig()->setPage_id(self::TASK_DETAIL);
    return $twig->render($this->getTEMPLATES_PATH() . 'task_gallery.html.twig', $twig_array, true, $_SESSION['lang'], self::TASK_DETAIL);
  }

  public function load_tasks_photos_html(array $twig_array){
    $twig = $this->getTwig();
    return $twig->render($this->getTEMPLATES_PATH() . 'task_photo.html.twig', $twig_array, true, $_SESSION['lang'], self::TASK_PHOTO);
  }

  public function load_pdf_photos_html(array $twig_array){
    $twig = $this->getTwig();
    $this->getTwig()->setPage_id(self::PHOTO_PDF_EXPORT);
    return $twig->render($this->getTEMPLATES_PATH() . 'photo_pdf_export.html.twig', $twig_array, true, $_SESSION['lang'], self::PHOTO_PDF_EXPORT);
  }

  public function getTwig(): \twig_controler{
    return $this->twig;
  }

  public function setTwig($twig){
    $this->twig = $twig;
  }

  protected function getTEMPLATES_PATH(){
    return $this->TEMPLATES_PATH;
  }

  protected function setTEMPLATES_PATH($TEMPLATES_PATH){
    $this->TEMPLATES_PATH = $TEMPLATES_PATH;
  }

  protected function getJS_PATH(){
    return $this->JS_PATH;
  }

  protected function setJS_PATH($JS_PATH){
    $this->JS_PATH = $JS_PATH;
  }

  protected function getCSS_PATH(){
    return $this->CSS_PATH;
  }

  protected function setCSS_PATH($CSS_PATH){
    $this->CSS_PATH = $CSS_PATH;
  }
}

?>
