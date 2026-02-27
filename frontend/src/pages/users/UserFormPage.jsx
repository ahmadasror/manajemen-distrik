import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Input, Button, Select, Switch, Card, Space, Spin, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { userApi } from '../../api/userApi';
import { ROLES } from '../../utils/constants';

export default function UserFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const isEdit = Boolean(id);

  useEffect(() => {
    if (isEdit) {
      setLoading(true);
      userApi.getById(id)
        .then((res) => {
          const user = res.data.data;
          form.setFieldsValue({
            ...user,
            roles: user.roles ? Array.from(user.roles) : [],
          });
        })
        .catch(() => message.error('Failed to load user'))
        .finally(() => setLoading(false));
    }
  }, [id, isEdit, form]);

  const onFinish = async (values) => {
    setSubmitting(true);
    try {
      if (isEdit) {
        const updateData = { ...values };
        if (!updateData.password) delete updateData.password;
        updateData.roles = new Set(values.roles) ? Array.from(values.roles) : values.roles;
        await userApi.update(id, updateData);
        message.success('Update request submitted for approval');
      } else {
        await userApi.create(values);
        message.success('Create request submitted for approval');
      }
      navigate('/users');
    } catch (err) {
      message.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  const roleOptions = Object.values(ROLES).map((r) => ({ label: r, value: r }));

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/users')}>Back</Button>
      </Space>
      <Card title={isEdit ? 'Edit User' : 'Create New User'}>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{ isActive: true, roles: [] }}
          style={{ maxWidth: 600 }}
        >
          {!isEdit && (
            <Form.Item
              name="username"
              label="Username"
              rules={[
                { required: true, message: 'Username is required' },
                { min: 3, message: 'Min 3 characters' },
                { pattern: /^[a-zA-Z0-9._-]+$/, message: 'Letters, numbers, dots, hyphens, underscores only' },
              ]}
            >
              <Input placeholder="Enter username" />
            </Form.Item>
          )}

          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: !isEdit, message: 'Email is required' },
              { type: 'email', message: 'Must be a valid email' },
            ]}
          >
            <Input placeholder="Enter email" />
          </Form.Item>

          <Form.Item
            name="password"
            label={isEdit ? 'New Password (leave blank to keep current)' : 'Password'}
            rules={isEdit ? [] : [
              { required: true, message: 'Password is required' },
              { min: 6, message: 'Min 6 characters' },
            ]}
          >
            <Input.Password placeholder="Enter password" />
          </Form.Item>

          <Form.Item
            name="fullName"
            label="Full Name"
            rules={[{ required: !isEdit, message: 'Full name is required' }]}
          >
            <Input placeholder="Enter full name" />
          </Form.Item>

          <Form.Item name="phone" label="Phone">
            <Input placeholder="Enter phone number" />
          </Form.Item>

          <Form.Item
            name="roles"
            label="Roles"
            rules={[{ required: true, message: 'At least one role is required' }]}
          >
            <Select mode="multiple" options={roleOptions} placeholder="Select roles" />
          </Form.Item>

          <Form.Item name="isActive" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={submitting}>
                {isEdit ? 'Submit Update' : 'Submit Create'}
              </Button>
              <Button onClick={() => navigate('/users')}>Cancel</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
