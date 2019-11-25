describe("L.GeometryUtil", function () {
	var map, control, container;

	it("geodesicArea", function () {
		expect(L.GeometryUtil.geodesicArea([
			{ lat: 0,  lng: 0 },
			{ lat: 0,  lng: 10 },
			{ lat: 10, lng: 10 },
			{ lat: 10, lng: 0 },
			{ lat: 0,  lng: 0 }
		])).to.eql(1232921098571.292);
	});

	describe("readableDistance", function () {
		it("nautical", function () {
			expect(L.GeometryUtil.readableDistance(1000, false, false, true)).to.eql('0.54 nm');
			expect(L.GeometryUtil.readableDistance(1500, false, false, true)).to.eql('0.81 nm');
		});

		it("feet", function () {
			expect(L.GeometryUtil.readableDistance(1000, false, true)).to.eql('3281 ft');
			expect(L.GeometryUtil.readableDistance(1500, false, true)).to.eql('4922 ft');
		});

		it("metric", function () {
			expect(L.GeometryUtil.readableDistance(1000, true)).to.eql('1000 m');
			expect(L.GeometryUtil.readableDistance(1500, true)).to.eql('1.50 km');
		});

		it("imperial", function () {
			expect(L.GeometryUtil.readableDistance(1609.3488537961)).to.eql('1760 yd');
			expect(L.GeometryUtil.readableDistance(1610.3488537961)).to.eql('1.00 miles');
		});
	});
});
