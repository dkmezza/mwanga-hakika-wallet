import 'package:flutter/material.dart';
import 'dashboard_tab.dart';
import 'transfer_screen.dart';
import 'requisitions_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;
  int _dashboardReloadTrigger = 0;

  void _onTabSelected(int i) {
    if (i == 0 && _selectedIndex != 0) {
      // Returning to Home — reload dashboard data
      setState(() { _selectedIndex = i; _dashboardReloadTrigger++; });
    } else {
      setState(() => _selectedIndex = i);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: [
          DashboardTab(
            reloadTrigger: _dashboardReloadTrigger,
            onSendMoney: () => setState(() => _selectedIndex = 1),
            onRequestTopUp: () => setState(() => _selectedIndex = 2),
          ),
          const TransferScreen(),
          const RequisitionsScreen(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: _onTabSelected,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.swap_horiz_outlined),
            selectedIcon: Icon(Icons.swap_horiz),
            label: 'Transfer',
          ),
          NavigationDestination(
            icon: Icon(Icons.add_circle_outline),
            selectedIcon: Icon(Icons.add_circle),
            label: 'Top-up',
          ),
        ],
      ),
    );
  }
}
