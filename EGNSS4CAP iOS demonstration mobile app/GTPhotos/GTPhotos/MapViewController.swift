//
//  MapViewController.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 04/11/2020.
//

import UIKit
import CoreData
import MapKit

class MapViewController: UIViewController, MKMapViewDelegate, PTManagerDelegate, CLLocationManagerDelegate{
    
    private static let ptManagerIdentifier = "MapViewController"
    
    var firstAppear = true
    
    let db = DB()
    var persistPhotos: [PersistPhoto]?
    
    let locationManager = CLLocationManager()
        
    @IBOutlet weak var _mkMapView: MKMapView!
    var mkMapView: MKMapView {
        return _mkMapView
    }
    
    var photoAnnotations: [Weak<PhotoMKAnnotation>] = []
    var azimAnnotations: [Weak<AzimuthMKAnnotation>] = []
    
    var ptManager: PTManager!
    var shownPath: PTPath?
    
    var viewDidAppearComplation : (() -> Void)?
    
    @IBOutlet weak var recordPathButton: UIButton!
    @IBOutlet weak var showPathsButton: UIButton!
    
    @IBOutlet weak var shownPathInfoView: UIView!
    @IBOutlet weak var shownPTINameLabel: UILabel!
    @IBOutlet weak var shownPTITitileLabel: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        mkMapView.layoutMargins = UIEdgeInsets(top: 20, left: 20, bottom: 20, right: 20)
        mkMapView.register(PhotoMKAnnotationView.self, forAnnotationViewWithReuseIdentifier: PhotoMKAnnotation.photoAnnotationIndetifier)
        mkMapView.register(AzimuthMKAnnotationView.self, forAnnotationViewWithReuseIdentifier: AzimuthMKAnnotation.azimuthAnnotationIndetifier)
        ptManager = PTManager.acquire(indetifier: Self.ptManagerIdentifier, ptManagerDelegate: self)
        mkMapView.delegate = self
        
        recordPathButton.layer.cornerRadius = 10
        recordPathButton.layer.shadowColor = UIColor.black.cgColor
        recordPathButton.layer.shadowOffset = CGSize(width: 3, height: 3)
        recordPathButton.layer.shadowOpacity = 0.3
        recordPathButton.layer.shadowRadius = 2.0
        
        showPathsButton.layer.cornerRadius = 10
        showPathsButton.layer.shadowColor = UIColor.black.cgColor
        showPathsButton.layer.shadowOffset = CGSize(width: 3, height: 3)
        showPathsButton.layer.shadowOpacity = 0.3
        showPathsButton.layer.shadowRadius = 2.0
        
        shownPathInfoView.layer.cornerRadius = 10
        shownPathInfoView.layer.shadowColor = UIColor.black.cgColor
        shownPathInfoView.layer.shadowOffset = CGSize(width: 3, height: 3)
        shownPathInfoView.layer.shadowOpacity = 0.3
        shownPathInfoView.layer.shadowRadius = 2.0
        
        setupLocationManager()
        setupUserLocation()
        
        loadPhotos()
        showPhotos()
        
        setPathTrackState(isTracking: ptManager.isTracking)
        
    }
    
    func setupLocationManager() {
        locationManager.requestWhenInUseAuthorization()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
    }
    
    func setupUserLocation() {
        mkMapView.showsUserLocation = true
        let buttonItem = MKUserTrackingBarButtonItem(mapView: mkMapView)
        self.navigationItem.rightBarButtonItem = buttonItem
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        if (viewDidAppearComplation != nil) {
            viewDidAppearComplation!()
            viewDidAppearComplation = nil
        }
        refreshShownPath()
        if shownPath == nil && firstAppear {
            mkMapView.showAnnotations(photoAnnotations.filter{$0.val != nil}.map{$0.val!}, animated: true)
        }
        
        firstAppear = false
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
    }
    
    private func loadPhotos() {
        persistPhotos = PersistPhoto.selectByActualUser(manageObjectContext: db.mainMOC)
    }
    
    private func showPhotos() {
        guard let persistPhotos = self.persistPhotos else {
            return
        }
        
        var i = 0
        for photo in persistPhotos {
            let photoAnnotation = PhotoMKAnnotation(parentViewController: self, persistPhoto: photo)
            mkMapView.addAnnotation(photoAnnotation)
            photoAnnotations.append(Weak(photoAnnotation))
            let azimAnnotation = AzimuthMKAnnotation(photo: photo)
            mkMapView.addAnnotation(azimAnnotation)
            azimAnnotations.append(Weak(azimAnnotation))
            i += 1
        }
    }
    
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        switch annotation {
        case is PhotoMKAnnotation:
            return mapView.dequeueReusableAnnotationView(withIdentifier: PhotoMKAnnotation.photoAnnotationIndetifier, for: annotation)
        case is AzimuthMKAnnotation:
            return mapView.dequeueReusableAnnotationView(withIdentifier: AzimuthMKAnnotation.azimuthAnnotationIndetifier, for: annotation)
        case is PTMKAnnotation:
            return ptManager.mapView(mapView, viewFor: annotation)
        default:
            return nil
        }
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        switch overlay {
        case is PTPolyline, is PTPolygon:
            return ptManager.mapView(mapView, rendererFor: overlay)
        default:
            return MKPolylineRenderer()
        }
    }
    
    func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        for ann in azimAnnotations {
            if ann.val == nil {
                continue
            }
            let annView = mapView.view(for: ann.val!)
            if annView != nil {
                UIView.animate(withDuration: 0.3, animations: {
                    annView!.transform = CGAffineTransform(rotationAngle: CGFloat(Util.deg2rad(degs: -self._mkMapView.camera.heading)))
                })
                
            }
        }
    }
    
    func PTSavePathError(error: Error) {
        let alert = UIAlertController(title: "Tracking Path Error", message: "An unexpected error occured during saving Path: \(error.localizedDescription)", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func PTNoPointError() {
        let alert = UIAlertController(title: "Tracking Path Error", message: "The path will not be saved because it does not contain any points.", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    @IBAction func recordToggle(_ sender: Any) {
        let willTrack = !ptManager.isTracking
        if willTrack {
            let alert = UIAlertController(title: "Path Name", message: "", preferredStyle: .alert)
            alert.addTextField { textField in
                textField.placeholder = "Type name of path"
            }
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in
                let textField = alert.textFields![0]
                let name = textField.text == nil || textField.text!.isEmpty ? "Untitled" : textField.text!
                self.ptManager.startTrack(pathName: name)
                self.setPathTrackState(isTracking: willTrack)
                self.showUserLocation()
            }))
            alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
            self.present(alert, animated: true, completion: nil)
        } else {
            self.setPathTrackState(isTracking: willTrack)
            self.showPath(ptPath: ptManager.stopTrack())
        }
    }
    
    func setPathTrackState(isTracking: Bool) {
        if isTracking {
            recordPathButton.setImage(#imageLiteral(resourceName: "icon_stop_act.png"), for: .normal)
            showPathInfo(ptPath: self.ptManager.acquirePTPathMainThread(), isRecording: isTracking)
        } else {
            recordPathButton.setImage(#imageLiteral(resourceName: "icon_record_act.png"), for: .normal)
            showPathInfo(ptPath: nil, isRecording: isTracking)
        }
    }
    
    func showPath(ptPath: PTPath?) {
        let isTracking = ptManager.isTracking
        if isTracking {
            let alert = UIAlertController(title: "Cannot Draw Path", message: "The path cannot be drawn if another one is currently beeing recorded.", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            self.present(alert, animated: false, completion: nil)
            return
        }
        shownPath = ptPath
        showPathInfo(ptPath: ptPath, isRecording: isTracking)
        ptManager.drawPathPolygon(ptPath: ptPath)
    }
    
    func showPathInfo(ptPath: PTPath?, isRecording: Bool) {
        if (ptPath == nil) {
            shownPathInfoView.isHidden = true
            return
        }
        shownPathInfoView.isHidden = false
        shownPTINameLabel.text = ptPath?.name
        if (isRecording) {
            shownPTITitileLabel.text = "Recording Path"
        } else {
            shownPTITitileLabel.text = "Showing Path"
        }
    }
    
    func refreshShownPath() {
        guard let path = shownPath else {
            return
        }
        var exist: Bool
        do {
            exist = try db.mainMOC.existingObject(with: path.objectID) != nil
        } catch {
            exist = false
        }
        if !exist {
            showPath(ptPath: nil)
        }
        
    }
    
    func showUserLocation() {
        mkMapView.setCenter(mkMapView.userLocation.coordinate, animated: true)
    }
    
    @IBAction func unwindToMapView(sender: UIStoryboardSegue) {
        guard let source = sender.source as? PathTrackTableViewController else {
            return
        }
        guard let path = source.selectedPath else {
            return
        }
        viewDidAppearComplation = {
            self.showPath(ptPath: path)
        }
    }
    
    @IBAction func moveToPathTap(_ sender: UITapGestureRecognizer) {
        if ptManager.isTracking {
            showUserLocation()
        } else if shownPath != nil{
            ptManager.moveToPolygon()
        }
    }
    
    @IBAction func mapType(_ sender: UISegmentedControl) {
        switch (sender.selectedSegmentIndex) {
                case 0:
                    _mkMapView.mapType = MKMapType.standard
                case 1:
                    _mkMapView.mapType = MKMapType.satellite
                default:
                    _mkMapView.mapType = MKMapType.hybrid
            }
    }
    
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
