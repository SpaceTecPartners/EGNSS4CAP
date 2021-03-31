import MapKit

class PTMKAnnotationView: MKAnnotationView {
    

    override var annotation: MKAnnotation? {
        willSet {
            guard let annotation = newValue as? PTMKAnnotation else {
                return
            }
            
            canShowCallout = true
            let view = annotation.ptPointInfoView
            let point = annotation.ptPoint
            image = #imageLiteral(resourceName: "icon_map_pin")
            view.latLabel.text = String(format: "%f", point.lat)
            view.lngLabel.text = String(format: "%f", point.lng)
            view.orderLabel.text = String(point.path!.points!.index(of: point) + 1)
            view.createdLabel.text = Util.prettyDate(date: point.created!) + " " + Util.prettyTime(date: point.created!)
            detailCalloutAccessoryView = view
            centerOffset = CGPoint(x: 0, y: (-image!.size.height / 2))
        }
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
