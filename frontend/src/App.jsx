import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box } from '@mui/material';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import ServerList from './pages/ServerList';
import ServerDetail from './pages/ServerDetail';
import ServerForm from './pages/ServerForm';
import ImportPage from './pages/ImportPage';
import SoftwareVersions from './pages/SoftwareVersions';
import MigrationPlanning from './pages/MigrationPlanning';
import ServerObsolescence from './pages/ServerObsolescence';
import Alerts from './pages/Alerts';
import Login from './pages/Login';
import { useAuth } from './context/AuthContext';

function App() {
  const { loading } = useAuth();

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        Chargement...
      </Box>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="servers" element={<ServerList />} />
        <Route path="servers/new" element={<ServerForm />} />
        <Route path="servers/:id" element={<ServerDetail />} />
        <Route path="servers/:id/edit" element={<ServerForm />} />
        <Route path="import" element={<ImportPage />} />
        <Route path="software-versions" element={<SoftwareVersions />} />
        <Route path="migrations" element={<MigrationPlanning />} />
        <Route path="server-obsolescence" element={<ServerObsolescence />} />
        <Route path="alerts" element={<Alerts />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
