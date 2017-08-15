import {Component} from "@angular/core";
import {SocketIO} from "nativescript-socketio";

let jsApp = require('./js/scalajs-fastopt.js'); // 2.9 MB
// let jsApp = require('./js/scalajs-opt.js'); // 550 KB

@Component({
    selector: "my-app",
    template: `
        <ActionBar title="My App" class="action-bar"></ActionBar>
    `
})
export class AppComponent {

    constructor() {
        let transportService = new jsApp.TransportService(new SocketIO("http://localhost:3000", {}));
    }
}
