import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  AppBar, Box, Drawer, IconButton, List, ListItem, ListItemButton,
  ListItemIcon, ListItemText, Toolbar, Typography, Button, Divider
} from '@mui/material';
import {
  Menu as MenuIcon, Dashboard as DashboardIcon, Storage as StorageIcon,
  Upload as UploadIcon, Logout as LogoutIcon, Download as DownloadIcon,
  Inventory as InventoryIcon, Schedule as ScheduleIcon, Warning as WarningIcon,
  NotificationsActive as AlertsIcon, FactCheck as ComplianceIcon
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { exportAPI } from '../services/api';

const drawerWidth = 240;

const menuItems = [
  { text: 'Tableau de bord', icon: <DashboardIcon />, path: '/' },
  { text: 'Serveurs', icon: <StorageIcon />, path: '/servers' },
  { text: 'Planning Migrations', icon: <ScheduleIcon />, path: '/migrations' },
  { text: 'Versions Logiciels', icon: <InventoryIcon />, path: '/software-versions' },
  { text: 'Obsolescence Serveur', icon: <WarningIcon />, path: '/server-obsolescence' },
  { text: 'Conformite CSR', icon: <ComplianceIcon />, path: '/compliance' },
  { text: 'Alertes', icon: <AlertsIcon color="error" />, path: '/alerts' },
  { text: 'Import', icon: <UploadIcon />, path: '/import' },
];

function Layout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, isAuthenticated } = useAuth();

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleExport = async (format) => {
    try {
      const response = format === 'csv'
        ? await exportAPI.exportCsv({})
        : await exportAPI.exportExcel({});

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `export_serveurs.${format === 'csv' ? 'csv' : 'xlsx'}`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Erreur export:', error);
    }
  };

  const drawer = (
    <Box>
      <Toolbar sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 1 }}>
        <Box
          component="img"
          src="/logo-edf.svg"
          alt="EDF"
          sx={{ height: 32, width: 'auto' }}
        />
        <Typography variant="h6" noWrap sx={{ fontWeight: 'bold', color: 'primary.main' }}>
          GED-PEI
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path}
              onClick={() => navigate(item.path)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Divider />
      <List>
        <ListItem disablePadding>
          <ListItemButton onClick={() => handleExport('csv')}>
            <ListItemIcon><DownloadIcon /></ListItemIcon>
            <ListItemText primary="Export CSV" />
          </ListItemButton>
        </ListItem>
        <ListItem disablePadding>
          <ListItemButton onClick={() => handleExport('excel')}>
            <ListItemIcon><DownloadIcon /></ListItemIcon>
            <ListItemText primary="Export Excel" />
          </ListItemButton>
        </ListItem>
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{ width: { sm: `calc(100% - ${drawerWidth}px)` }, ml: { sm: `${drawerWidth}px` } }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Gestion des Serveurs GED-PEI
          </Typography>
          {isAuthenticated() ? (
            <>
              <Typography variant="body2" sx={{ mr: 2 }}>
                {user?.fullName || user?.username}
              </Typography>
              <Button color="inherit" onClick={handleLogout} startIcon={<LogoutIcon />}>
                Deconnexion
              </Button>
            </>
          ) : (
            <Button color="inherit" onClick={() => navigate('/login')}>
              Connexion
            </Button>
          )}
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          mt: 8,
          minHeight: '100vh',
          bgcolor: 'background.default'
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}

export default Layout;
