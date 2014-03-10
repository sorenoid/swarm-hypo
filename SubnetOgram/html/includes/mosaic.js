function makeTable(rowCount, colCount) {

	rows=new Array();
	cols=new Array();
	
	tab=document.createElement('table');
	tab.setAttribute('id','newtable');

	tbo=document.createElement('tbody');
	
	for(c=0;c<rowCount;c++){
		row[c]=document.createElement('tr');
		
		for(k=0;k<colCount;k++) {
			cell[k]=document.createElement('td');
			cont=document.createTextNode((c+1)*(k+1))
			cell[k].appendChild(cont);
			row[c].appendChild(cell[k]);
		}
		tbo.appendChild(row[c]);
	}
	tab.appendChild(tbo);
	document.getElementById('mosaicTable').appendChild(tab);
}
