import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  static const platform = MethodChannel('com.spike.foco/settings');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'Spike Técnico — Modo Foco',
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),

              const SizedBox(height: 32),

              ElevatedButton(
                onPressed: () async {
                  await platform.invokeMethod('openAccessibilitySettings');
                },
                child: const Text('Abrir Acessibilidade'),
              ),

              const SizedBox(height: 16),

              ElevatedButton(
                onPressed: () async {
                  await platform.invokeMethod('openOverlaySettings');
                },
                child: const Text('Abrir Permissão de Overlay'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
