function toggle_visibility(id) {
   var e = document.getElementById(id);
   if(e.style.display == 'block')
      e.style.display = 'none';
   else
      e.style.display = 'block';
}

function loadAbsoluteTime (form) {
    var year = form.year.value;
    var month = form.month.value - 1;
    var day = form.day.value;
    var hour = form.hour.value;
    var minute = form.minute.value;

    var requestedDate = new Date(year, month, day, hour, minute, 0, 0);
    var timeZoneOffset = form.timeZoneOffset.value;

    requestedDate.setTime(parseInt(requestedDate.getTime()) - parseInt(timeZoneOffset));

    var pathDateFormat = new SimpleDateFormat(form.pathDateFormat.value);
    var nameDateFormat = new SimpleDateFormat(form.nameDateFormat.value);

    var pathToRoot = form.pathToRoot.value;
    var subnetName = form.subnetName.value;
    var fileExtension = form.fileExtension.value;

    var location = pathToRoot;

    if (form.networkName != null) {
        location += form.networkName.value + "/";
    }

    location += subnetName + "/" + pathDateFormat.format(requestedDate) + subnetName + nameDateFormat.format(requestedDate) + fileExtension;

    window.location = location;
}

