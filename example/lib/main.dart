import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:image_gallery_saver/image_gallery_saver.dart';

import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';
import 'package:dio/dio.dart';
import 'dart:ui' as ui;

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Save image to gallery',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  GlobalKey _globalKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    PermissionHandler().requestPermissions(<PermissionGroup>[
      PermissionGroup.storage, // 在这里添加需要的权限
    ]);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Save image to gallery"),
      ),
      body: Container(
        margin: EdgeInsets.only(top: 10),
        child: _buildBody(),),
    );
  }

  _buildBody() {
    return Center(
      child: Column(
        children: <Widget>[
          RepaintBoundary(
            key: _globalKey,
            child: Container(
              width: 200,
              height: 200,
              color: Colors.red,
            ),
          ),
          Container(
            padding: EdgeInsets.only(top: 15),
            child: RaisedButton(
              onPressed: _saveScreen,
              child: Text("保存页面图片到相册"),
            ),
            width: 200,
            height: 44,
          ),
          Container(
            padding: EdgeInsets.only(top: 15),
            child: RaisedButton(
              onPressed: _saveNetworkImage,
              child: Text("保存网络图片到相册"),
            ),
            width: 200,
            height: 44,
          ),
          Container(
            padding: EdgeInsets.only(top: 15),
            child: RaisedButton(
              onPressed: _saveVideo,
              child: Text("保存网络视频到相册"),
            ),
            width: 200,
            height: 44,
          ),
        ],
      ),
    );
  }

  _saveScreen() async {
    RenderRepaintBoundary boundary =
        _globalKey.currentContext.findRenderObject();
    ui.Image image = await boundary.toImage();
    ByteData byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    final result =
        await ImageGallerySaver.saveImage(byteData.buffer.asUint8List());
    print(result);
  }

  _saveNetworkImage() async {
    var response = await Dio().get(
        "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1585829049863&di=0fe537a3a3aa640cbeac2ec0a753e8c9&imgtype=0&src=http%3A%2F%2Ff.hiphotos.baidu.com%2Fbaike%2Fs%253D220%2Fsign%3De03c0bc3f3deb48fff69a6dcc01e3aef%2Fc83d70cf3bc79f3de51a7592baa1cd11738b299c.jpg",
        options: Options(responseType: ResponseType.bytes));
    final result =
        await ImageGallerySaver.saveImage(Uint8List.fromList(response.data));
    print(result);
  }

  _saveVideo() async {
    var appDocDir = await getTemporaryDirectory();
    String savePath = appDocDir.path + "/temp.mp4";
    await Dio().download(
        "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4", savePath);
    final result = await ImageGallerySaver.saveFile(savePath);
    print(result);
  }
}
