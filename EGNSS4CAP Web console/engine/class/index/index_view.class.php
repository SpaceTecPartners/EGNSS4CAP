<?php
namespace index;

class index_view{

  protected $TEMPLATES_PATH;
  protected $JS_PATH;
  protected $CSS_PATH;
  public const AGENCY_FARMERS_PAGE_ID = 2;
  public const TASKS_PAGE_ID = 3;
  public const GALLERY_PAGE_ID = 7;
  public const PATHS_PAGE_ID = 8;
  public const AGENCY_PAGE_ID = 9;
  public const PA_OFFICERS_PAGE_ID = 10;
  public const RELEASE_NOTES_PAGE_ID = 11;

  private const MY_PATH = 'index' . DIRECTORY_SEPARATOR;

  private $twig;
  public function __construct($path = ''){
    $this->setTwig(new \twig_controler());
    $this->setTEMPLATES_PATH(self::MY_PATH.$path);
    $this->setJS_PATH('js' . DIRECTORY_SEPARATOR . self::MY_PATH . $path);
    $this->setCSS_PATH('css' . DIRECTORY_SEPARATOR . self::MY_PATH . $path);
  }

  public function load_index_farmers_tasks_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'index.js');
    $twig->pushJavascript($this->getJS_PATH() . 'index_task_list.js');
    $twig->use_map_js();
    $this->getTwig()->setPage_id(self::TASKS_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_farmers_tasks.html.twig', $twig_array, true, $_SESSION['lang'], self::TASKS_PAGE_ID);
  }

  public function load_index_gallery_unassigned_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'index.js');
    $twig->pushJavascript($this->getJS_PATH() . 'index_users_gallery.js');
    $twig->use_map_js();
    $this->getTwig()->setPage_id(self::GALLERY_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_gallery_unassigned.html.twig', $twig_array, true, $_SESSION['lang'], self::GALLERY_PAGE_ID);
  }

  public function load_index_user_paths_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'index.js');
    $twig->pushJavascript($this->getJS_PATH() . 'index_user_paths.js');
    $twig->use_map_js();
    $this->getTwig()->setPage_id(self::PATHS_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_user_paths.html.twig', $twig_array, true, $_SESSION['lang'], self::PATHS_PAGE_ID);
  }

  public function load_index_release_notes_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $this->getTwig()->setPage_id(self::RELEASE_NOTES_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_release_notes.html.twig', $twig_array, true, $_SESSION['lang'], self::RELEASE_NOTES_PAGE_ID);
  }

  public function load_index_agency_farmers_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'index.js');
    $twig->pushJavascript($this->getJS_PATH() . 'index_users.js');
    $twig->use_map_js();
    $this->getTwig()->setPage_id(self::AGENCY_FARMERS_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_agency_farmers.html.twig', $twig_array, true, $_SESSION['lang'], self::AGENCY_FARMERS_PAGE_ID);
  }

  public function load_index_agency_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'index.js');
    $twig->pushJavascript($this->getJS_PATH() . 'index_agency.js');
    //$twig->use_map_js();
    $this->getTwig()->setPage_id(self::AGENCY_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_agency.html.twig', $twig_array, true, $_SESSION['lang'], self::AGENCY_PAGE_ID);
  }

  public function load_index_pa_officers_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_jquery();
    $twig->pushJavascript($this->getJS_PATH() . 'index.js');
    $twig->pushJavascript($this->getJS_PATH() . 'index_pa_officers.js');
    //$twig->use_map_js();
    $this->getTwig()->setPage_id(self::PA_OFFICERS_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_pa_officers.html.twig', $twig_array, true, $_SESSION['lang'], self::PA_OFFICERS_PAGE_ID);
  }

  public function load_table_of_pa_officers(array $twig_array){
    $twig = $this->getTwig()->setPage_id(self::PA_OFFICERS_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_pa_officers_table.html.twig', $twig_array, false, $_SESSION['lang'], (self::PA_OFFICERS_PAGE_ID));
  }

  public function load_table_of_agency(array $twig_array){
    $twig = $this->getTwig();
    return $twig->render($this->getTEMPLATES_PATH() . 'index_agency_table.html.twig', $twig_array);
  }

  public function load_table_of_users(array $twig_array){
    $twig = $this->getTwig()->setPage_id(self::AGENCY_FARMERS_PAGE_ID);
    return $twig->render($this->getTEMPLATES_PATH() . 'index_agency_farmers_table.html.twig', $twig_array, false, $_SESSION['lang'], self::AGENCY_FARMERS_PAGE_ID);
  }

  public function load_table_of_tasks(array $twig_array){
    $twig = $this->getTwig();
    return $twig->render($this->getTEMPLATES_PATH() . 'index_farmers_tasks_table.html.twig', $twig_array, false, $_SESSION['lang'], self::TASKS_PAGE_ID);
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
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
